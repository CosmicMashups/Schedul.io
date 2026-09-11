# Building and Deploying a Java System From Scratch

A general-purpose manual for taking a Spring Boot (or similar JVM) backend, plus one or more
static/SPA frontends, from local code to a live, publicly reachable deployment — written up
from the actual process and failures worked through deploying this project (Java 25 / Spring
Boot 4 backend, three React/Vite frontends, Supabase Postgres, Render, Vercel). The specific
services named are one valid combination, not the only one; the *steps and reasoning* are the
reusable part.

## 1. Decide the shape of the deployment before touching a dashboard

A JVM backend and a static frontend have fundamentally different hosting needs, and conflating
them wastes time:

- **A Spring Boot (or any long-running JVM) backend** needs a host that runs a persistent
  process: it holds a database connection pool, may run background/scheduled jobs, and takes
  real seconds to boot (classpath scanning, Hibernate bootstrap, migrations). It does **not**
  fit serverless/edge function platforms (Vercel, Netlify, Cloudflare Workers) — those expect
  short-lived, stateless invocations. Use a platform built for containers/long-running
  services: Render, Railway, Fly.io, a VPS, or a traditional cloud VM/container service.
- **A static SPA build** (Vite/CRA/Next static export, etc.) is exactly what serverless-style
  platforms are built for — deploy it to Vercel, Netlify, Cloudflare Pages, or similar.
- **The database** is a separate concern from both. A managed Postgres provider (Supabase,
  Neon, RDS, etc.) or a database add-on from your backend's host.

Pick one service per concern, get each one's connection details, *then* start deploying —
figuring out the shape as you go means re-doing config repeatedly.

## 2. Containerize the backend

Unless your backend platform has first-class support for your exact JVM/build-tool version, a
Dockerfile is the most portable and predictable way to build a Java service. Use a multi-stage
build: one stage with the full JDK + build tool to compile, a second, slimmer stage with just
a JRE to run.

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
```

Notes worth internalizing, not just copying:

- **If no build-tool wrapper (`mvnw`/`gradlew`) is checked into the repo**, don't try to
  install one inside the Dockerfile — just use a base image that already bundles the build
  tool (`maven:<version>-eclipse-temurin-<jdk-version>`), as above. Trying to invoke a
  nonexistent `./mvnw` is a common, easy-to-hit dead end.
- **Copy `pom.xml`/`build.gradle` and run the dependency-resolution step *before* copying
  `src/`.** Docker layer caching means a source-only change won't force a full dependency
  re-download on the next build — meaningfully faster iteration.
- **Run as a non-root user** in the final stage (`addgroup`/`adduser` + `USER`) — a small,
  standard hardening step, not specific to this stack.
- **Forward the platform's injected `$PORT` into your app's own port property** (here,
  `-Dserver.port=${PORT:-8080}`). Most container platforms (Render included) assign the
  external port dynamically via an env var rather than letting you hardcode 8080 — if your
  entrypoint ignores it, the platform's health checker can't find an open port and the deploy
  hangs or fails with something like "No open ports detected."
- Add a `.dockerignore` (`target/`, `.git/`, IDE folders, logs) so the build context stays
  small and the image doesn't pick up local build artifacts.

## 3. Make configuration entirely environment-variable driven

Before deploying anywhere, audit `application.yml`/`application.properties` (or equivalent)
and confirm every environment-specific value — database host/credentials, cache host, external
API keys, secrets — is a `${VAR:default}`-style placeholder, never a hardcoded value. Local
defaults should point at localhost/dev values so nothing breaks for local development; every
placeholder should be overridable purely by setting an OS environment variable, since that's
the only channel most hosting platforms give you (no `.env` file support for a JVM app the way
frontend tooling has it — you set real environment variables in the platform's dashboard).

If you need to append provider-specific connection parameters (SSL mode, for example) without
hardcoding them for every environment, thread an empty-by-default variable into the connection
string itself rather than duplicating the whole URL per environment:

```yaml
datasource:
  url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mydb}${DB_SSL_PARAMS:}
```

Locally, `DB_SSL_PARAMS` stays unset and the URL is unchanged; in production, set
`DB_SSL_PARAMS=?sslmode=require` (or whatever your provider needs) without touching the base
URL pattern or your local setup.

## 4. Provision the managed database — and get the *reachable* connection details

"Create a database" is the easy part; getting the *right* connection string for your hosting
platform is where time actually goes, because managed Postgres providers commonly expose
**multiple connection modes** that are not interchangeable:

- **Direct connection** — a straight connection to the database instance. Modern managed
  Postgres providers increasingly make this **IPv6-only** unless you pay for an IPv4 add-on.
  If your backend host's outbound networking doesn't support IPv6 (common — many platforms
  don't), a direct connection will **time out at the TCP level** (a `SocketTimeoutException`
  or "Connect timed out," not an authentication or config error). This is a networking
  reachability problem, not a credentials problem — don't waste time re-checking passwords.
- **Transaction-mode connection pooler** (e.g., PgBouncer in transaction mode) — reachable over
  IPv4, but does **not** guarantee the same underlying connection is held across statements
  within one client transaction. ORMs that rely on server-side prepared statements (Hibernate
  is one) can break in subtle, hard-to-diagnose ways against this mode.
- **Session-mode connection pooler** — the practical answer when your host can't reach a
  direct/IPv6 connection: IPv4-reachable, *and* each client gets a connection held for its
  whole session, so it behaves like a direct connection as far as your ORM is concerned. If
  your provider offers this mode, prefer it for a traditional backend that manages its own
  connection pool (HikariCP, etc.) — you don't need the provider's pooler for pooling itself
  (your app already pools), you need it specifically for the IPv4 reachability.

Also check whether the pooled connection mode changes the **username format** (some providers
suffix a project/tenant identifier onto the username for pooled connections but not for direct
ones) — using the direct-connection username against a pooler endpoint is a quiet way to get
an authentication failure that looks unrelated to the actual cause.

**Practical order of operations:** try connecting from your actual deployment platform (not
just your laptop) early, before you've built anything else on top — a networking-mode mismatch
is much cheaper to discover before you've configured five other things.

## 5. Get schema migrations right on a database that isn't a blank slate

If you're using a migration tool (Flyway, Liquibase, etc.) with a "baseline on first run"
style feature: understand exactly what "non-empty" means to that tool before pointing it at a
managed database. A fresh database from a hosted provider is often **not** a truly empty
schema from the migration tool's point of view — it may already have extensions registered,
default roles, or reserved schemas that make the tool's "does this look pre-existing?" check
answer "yes."

The dangerous failure mode: the tool decides the schema is pre-existing and unmanaged, and
instead of running your migrations, it silently **marks your first migration(s) as already
applied without ever running them** — then your second migration fails with confusing errors
about tables/columns that don't exist, because the first migration that was supposed to create
them never actually ran.

The fix is tool-specific, but the general shape is the same: force the "baseline" marker to
sit *below* your very first real migration, so baselining (if it triggers at all) never causes
a real migration to be skipped. For Flyway specifically:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0   # below every real migration (V1, V2, ...) — nothing gets skipped
```

If you've already hit this and the migration tool's own bookkeeping table now has a bad
baseline record in it, you generally need to manually drop that bookkeeping table (e.g.
`DROP TABLE flyway_schema_history;` for Flyway) before redeploying with the corrected config —
fixing the config alone doesn't retroactively undo a bad baseline that already happened.

## 6. Generate real secrets — don't type them by hand

Any signing key, encryption key, or secret the framework turns into cryptographic key material
(JWT signing secrets are the classic case) usually has a **minimum required byte length**, and
libraries frequently derive that length from the **raw byte length of the string you provide**
— not from some more forgiving interpretation of "seems long enough." A short, hand-typed
password-style string is a common way to fail this silently until the app tries to actually
use the key and throws a weak-key error at startup.

Generate secrets properly and paste the entire output:

```bash
openssl rand -hex 32     # 64 characters = 256 bits, safe for most HMAC-SHA uses
openssl rand -base64 48  # a longer alternative if the target expects base64
```

Check what your specific library expects (raw bytes of the string vs. base64-decoded bytes)
before assuming a given command's output is long enough — mismatching that assumption is
exactly the kind of thing that produces the URL/key-length class of bug covered here.

## 7. Deploy the backend

With the Dockerfile, environment-variable-driven config, working database connection string,
and a real secret in hand:

1. Create the web service on your chosen platform, pointing it at the backend's subdirectory
   (if it's part of a monorepo) and letting it build from the Dockerfile.
2. Set every environment variable the app needs — database (host/port/name/user/password/ssl),
   cache (if used), the generated secret(s), and anything else `application.yml` references.
3. Set a health check path if the platform supports it (Spring Boot Actuator's
   `/actuator/health` is the standard choice) so the platform can tell a genuinely broken
   deploy apart from one that's just still booting.
4. Watch the first deploy's logs end-to-end rather than assuming success from "the build
   finished" — a Docker image can build perfectly and the *application* can still fail to
   start (bad DB connection, bad secret, migration failure). The real signal is the
   framework's own "started successfully" log line and the platform's own "your service is
   live" confirmation, not just a green build step.

**A subtlety worth calling out explicitly:** a health check reporting unhealthy doesn't always
mean the service is actually broken. If your health check aggregates multiple components
(database, cache, message broker, etc.) and one of them isn't provisioned yet but also isn't
actually used by any code path yet, the health check can report `DOWN` while every real
endpoint works perfectly. Don't treat an aggregate health status as automatically equivalent
to "nothing works" — check whether the failing component is load-bearing before spending time
"fixing" something that isn't actually blocking anything.

## 8. Deploy the frontend(s)

For a Vite (or similar) SPA on a platform like Vercel:

1. One project per app if you have multiple frontends talking to the same backend (e.g.
   separate patient-facing and staff-facing apps) — each gets its own build config, env vars,
   and URL.
2. Point the platform's "root directory" setting at the specific frontend's folder if it's
   part of a monorepo.
3. Add a rewrite rule so client-side routing survives a page refresh or a direct link to a
   deep route — without it, a router-based SPA 404s on anything but the exact root path:
   ```json
   {
     "framework": "vite",
     "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
   }
   ```
4. Set the frontend's env var(s) pointing at the backend's now-live URL (e.g.
   `VITE_API_BASE_URL`), plus anything else the frontend needs to function without an
   authenticated session yet (e.g. a default tenant/org identifier for a multi-tenant app's
   public, pre-login pages, since there's no session to carry that context yet).
5. **Confirm the production build actually succeeds, not just the dev server.** A dev server
   (`vite dev`, `next dev`, etc.) commonly runs a faster, looser transform that does not fully
   typecheck the code the way a production build's typechecking step does. Something can run
   fine locally in dev mode and still fail the platform's build — most often a missing
   ambient-types declaration file (e.g. Vite's `import.meta.env` typing needs a
   `vite-env.d.ts` with `/// <reference types="vite/client" />` present, or TypeScript doesn't
   know that property exists) or a type error that dev mode's transform never actually checks.
   **Run the exact build command the platform will run, locally, before pushing** — it's the
   cheapest way to catch this class of bug before burning a deploy cycle on it.

## 9. Cross-origin access between frontend(s) and backend

Once your frontend(s) live on a different origin (a different domain, including different
subdomains, than the backend), the backend needs CORS configured to allow those origins —
otherwise the browser blocks the requests even though the backend itself would happily answer
them. Configure this deliberately (explicit allowed origins in real production use — a
wildcard is fine to get moving during initial setup, but should be tightened before this is
genuinely handling real traffic) rather than discovering it's missing via a wall of blocked
requests in the browser console.

## 10. A debugging checklist for "it worked locally, it doesn't work deployed"

Roughly the order to check things in, cheapest/most-likely first:

1. **Read the actual deploy log start to finish**, not just the last few lines — the real
   error is often several screens above a downstream symptom (e.g. a Tomcat/web-server startup
   failure log is frequently just the *consequence* of an earlier bean-creation failure that
   scrolled past).
2. **Is the failure a network-level timeout, or an application-level error?** A raw socket/TCP
   timeout points at reachability (wrong host, wrong network mode, firewall) — no amount of
   fixing credentials helps that. An application-level exception (auth failure, malformed
   query, missing bean) points at configuration or code.
3. **Did every environment variable actually get set, and set to the right *kind* of value?**
   A full URL where a bare hostname was expected, a pooler port where a direct port was
   expected, a plaintext secret where a sufficiently-long random one was expected — these all
   produce confusing downstream errors that don't obviously point back at the env var itself.
4. **Is the database schema actually in the state the app expects?** Especially after any
   failed migration attempt — check what state was left behind before assuming a fix alone is
   enough.
5. **Is the thing reporting "unhealthy" actually load-bearing**, or is it an unused/optional
   dependency whose health check is failing independent of whether the app actually works?

---

This manual mirrors the actual sequence of issues resolved deploying this repository's
backend (Spring Boot on Render, containerized) and three frontends (React/Vite on Vercel),
against a Supabase-hosted Postgres database — see `backend/README.md`'s "Deploying to Render"
section for those exact, project-specific values and commands.
