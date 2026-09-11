# Building and Deploying a Java System From Scratch

A general-purpose manual for taking a Spring Boot (or similar JVM) backend, plus one or more
static/SPA frontends, from local code to a live, publicly reachable deployment — written up
from the actual process and failures worked through deploying this project (Java 25 / Spring
Boot 4 backend, three React/Vite frontends, Supabase Postgres, Render, Vercel). The specific
services named are one valid combination, not the only one; the *steps and reasoning* are the
reusable part, and each section explains *why*, not just *what*, so you can adapt it to a
different host, database provider, or frontend platform.

**How to use this document:** read it once end-to-end before starting — most of the time lost
in a first deployment comes from doing steps out of order (e.g. building infrastructure before
confirming the database is even reachable from where the app will actually run). Then use it
as a checklist while you work.

## Table of contents

0. [Prerequisites](#0-prerequisites)
1. [Decide the shape of the deployment before touching a dashboard](#1-decide-the-shape-of-the-deployment-before-touching-a-dashboard)
2. [Containerize the backend](#2-containerize-the-backend)
3. [Make configuration entirely environment-variable driven](#3-make-configuration-entirely-environment-variable-driven)
4. [Provision the managed database](#4-provision-the-managed-database--and-get-the-reachable-connection-details)
5. [Get schema migrations right on a database that isn't a blank slate](#5-get-schema-migrations-right-on-a-database-that-isnt-a-blank-slate)
6. [Secrets: generation, storage, and rotation](#6-secrets-generation-storage-and-rotation)
7. [Deploy the backend](#7-deploy-the-backend)
8. [Deploy the frontend(s)](#8-deploy-the-frontends)
9. [Cross-origin access between frontend(s) and backend](#9-cross-origin-access-between-frontends-and-backend)
10. [Observability: logs, health, and knowing it's actually working](#10-observability-logs-health-and-knowing-its-actually-working)
11. [Free-tier and low-cost hosting realities](#11-free-tier-and-low-cost-hosting-realities)
12. [Redeploying, rolling back, and iterating safely](#12-redeploying-rolling-back-and-iterating-safely)
13. [Security checklist before calling it "production"](#13-security-checklist-before-calling-it-production)
14. [Debugging checklist: "it worked locally, it doesn't work deployed"](#14-debugging-checklist-it-worked-locally-it-doesnt-work-deployed)
15. [Appendix: reference snippets](#15-appendix-reference-snippets)

---

## 0. Prerequisites

Before any of this is deployment-specific, make sure the basics are solid locally — every
deployment problem downstream is harder to diagnose if you're not certain the *code* is
correct first.

- **The app runs and passes its own tests locally**, against a local instance of whatever
  database/cache it needs (Docker Compose is the standard way to spin these up
  reproducibly — see the appendix for a starter file). Don't attempt your first-ever
  successful run of the app *during* a cloud deployment; you'll have three unknowns
  (code, environment, network) instead of one.
- **Version control is initialized and the remote is set up** (GitHub/GitLab/etc.) — almost
  every hosting platform deploys by pulling from a git repository, either on push or on
  demand. If you haven't already: `git init`, commit, create the remote repo, `git remote add
  origin <url>`, `git push -u origin main`.
- **A `.gitignore` that excludes build output, local secrets, and IDE files** — build
  directories (`target/`, `dist/`, `node_modules/`), `.env` files with real values (keep a
  committed `.env.example` with placeholder values instead), and editor-specific folders
  (`.idea/`, `.vscode/`). Committing a real secret to git history is a mistake that isn't
  undone by deleting the file in a later commit — the value is still in history. If it
  happens, rotate the secret; don't just delete the line.
- **You know your JVM and build-tool versions precisely** (e.g. "Java 25, Maven"). Every base
  image, buildpack, and platform runtime selection later depends on getting this exact —
  a mismatch (e.g. compiling with a newer JDK than the deployment image provides) produces
  class-file-version errors that are easy to misdiagnose as something else.

## 1. Decide the shape of the deployment before touching a dashboard

A JVM backend and a static frontend have fundamentally different hosting needs, and conflating
them wastes time:

- **A Spring Boot (or any long-running JVM) backend** needs a host that runs a persistent
  process: it holds a database connection pool, may run background/scheduled jobs, and takes
  real seconds to boot (classpath scanning, Hibernate bootstrap, migrations). It does **not**
  fit serverless/edge function platforms (Vercel, Netlify, Cloudflare Workers) — those expect
  short-lived, stateless invocations, typically with execution time limits far shorter than a
  JVM's cold-start time alone. Use a platform built for containers/long-running services:
  Render, Railway, Fly.io, a VPS, or a traditional cloud VM/container service (ECS, Cloud Run,
  App Engine, Azure App Service, etc.).
- **A static SPA build** (Vite/CRA/Next static export, etc.) is exactly what serverless-style
  platforms are built for — deploy it to Vercel, Netlify, Cloudflare Pages, or similar. These
  platforms give you a global CDN, instant rollbacks, and preview deployments per branch/PR
  essentially for free, which a container host generally doesn't offer for static assets.
- **The database** is a separate concern from both, and should almost always be a managed
  service rather than something you run yourself in a container — a managed Postgres provider
  (Supabase, Neon, RDS, Cloud SQL, etc.) or a database add-on from your backend's host handles
  backups, patching, and failover in ways that are genuinely hard to replicate correctly
  yourself, especially early on.
- **A cache/message broker** (Redis, Kafka, RabbitMQ, etc.), if your app uses one, follows the
  same logic — a managed add-on, not a container you operate. If your app has a dependency on
  the classpath but doesn't actually *use* it yet (see §10 for why this matters), you can
  often defer provisioning it entirely until the code that needs it exists.

Pick one service per concern, get each one's connection details, *then* start deploying —
figuring out the shape as you go means re-doing config repeatedly. Sketch it out, even just as
a short list:

```
Backend:   Render (Docker) — connects to →
Database:  Supabase Postgres (Session pooler)
Cache:     Render Key Value (Redis-compatible)
Frontend:  Vercel × N projects — each calls the backend's public URL
```

## 2. Containerize the backend

Unless your backend platform has first-class native support for your exact JVM/build-tool
version and version pinning granularity you trust, a Dockerfile is the most portable and
predictable way to build a Java service — it guarantees the exact same build and runtime
environment locally, in CI, and on the hosting platform, rather than trusting each of those
to independently interpret a "detected framework" correctly.

Use a **multi-stage build**: one stage with the full JDK + build tool to compile, a second,
much slimmer stage with just a JRE (no compiler, no build tool, no source) to actually run the
app. This keeps the final image small (faster deploys, smaller attack surface) without giving
up anything at build time.

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy only the dependency manifest first — Docker caches each layer by its inputs, so if
# pom.xml hasn't changed, this whole dependency-resolution step is skipped on the next build
# even though src/ changed. This is the single biggest build-speed win available here.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as a dedicated non-root user — standard container hardening, costs nothing.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Only the built artifact crosses into this stage — no JDK, no Maven, no source tree.
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080
# ${PORT:-8080} — see the note below on why this matters.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
```

Notes worth internalizing, not just copying:

- **If no build-tool wrapper (`mvnw`/`gradlew`) is checked into the repo**, don't try to
  install one inside the Dockerfile — just use a base image that already bundles the build
  tool (`maven:<maven-version>-eclipse-temurin-<jdk-version>`, or the Gradle equivalent), as
  above. Trying to invoke a nonexistent `./mvnw` is a common, easy-to-hit dead end, and adding
  a wrapper after the fact is also a legitimate fix (`mvn wrapper:wrapper`) if you'd rather
  standardize on that instead.
- **Pin exact versions on both stages' base images**, not just "latest" — an unpinned tag can
  silently change under you between builds (a new JDK patch release, a new Maven version),
  and the failure mode is usually "worked yesterday, doesn't today" with no code change to
  point at. `maven:3.9-eclipse-temurin-25` is already reasonably pinned; go further
  (`maven:3.9.9-eclipse-temurin-25.0.1`, or a full `@sha256:...` digest) if you want fully
  reproducible builds.
- **Copy the dependency manifest and resolve dependencies *before* copying `src/`.** Covered
  above, worth repeating: this is the difference between a 5-second incremental build and a
  multi-minute one on every single code change.
- **Run as a non-root user** in the final stage (`addgroup`/`adduser` + `USER`) — a small,
  standard hardening step that costs nothing and is worth doing by default.
- **Forward the platform's injected `$PORT` into your app's own port property** (here,
  `-Dserver.port=${PORT:-8080}`). Most container platforms (Render included) assign the
  *external* port dynamically via an environment variable rather than letting you hardcode
  8080 — if your entrypoint ignores it and always binds 8080 regardless, the platform's health
  checker can't find an open port on the port it expects, and the deploy hangs or fails with
  something like "No open ports detected, continuing to scan..." repeated in the logs. The
  `${PORT:-8080}` fallback means the same image still works fine locally where `$PORT` isn't
  set.
- **Add a `.dockerignore`** so the build context stays small and the image doesn't pick up
  local build artifacts or secrets that happen to sit in the working directory:
  ```
  target/
  *.log
  .idea/
  .vscode/
  *.iml
  .git/
  ```
- **Multi-stage build vs. buildpacks vs. platform-native builds:** some platforms (Render
  included, along with Railway, Heroku, Google Cloud Run) can also build directly from source
  without a Dockerfile, using Cloud Native Buildpacks or a similar auto-detection mechanism.
  These can work well and save you writing the Dockerfile at all, but you trade away precise
  control over the JDK version, build-tool version, and final image contents — worth trying
  first for a quick prototype, worth replacing with an explicit Dockerfile once you've hit any
  version-mismatch surprises from it, or whenever you want the build to behave identically
  across your laptop, CI, and the hosting platform.

## 3. Make configuration entirely environment-variable driven

Before deploying anywhere, audit `application.yml`/`application.properties` (or the
equivalent for your framework) and confirm every environment-specific value — database
host/credentials, cache host, external API keys, feature-flag toggles, secrets — is a
`${VAR:default}`-style placeholder, never a hardcoded value. Local defaults should point at
localhost/dev values so nothing breaks for local development; every placeholder should be
overridable purely by setting an OS environment variable, since that's the only channel most
hosting platforms give you (no `.env` file support for a JVM app the way frontend build
tooling has it — you set real environment variables directly in the platform's dashboard, or
via its CLI/API).

A representative slice of what this looks like for a Spring Boot app talking to Postgres,
Redis, and issuing its own JWTs:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mydb}${DB_SSL_PARAMS:}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:app}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      # password is intentionally NOT given a placeholder with a default here — Spring Boot's
      # relaxed env-var binding still lets you set SPRING_DATA_REDIS_PASSWORD directly even
      # without a corresponding ${...} placeholder in this file, so it's fine to omit entries
      # you don't need a local default for.

app:
  security:
    jwt-secret: ${JWT_SECRET:dev-only-insecure-default-do-not-use-in-prod}
```

Why the `${DB_SSL_PARAMS:}` pattern specifically: if you need to append provider-specific
connection parameters (SSL mode is the most common one) without hardcoding a different full
URL per environment, thread an empty-by-default variable into the connection string itself:

```yaml
datasource:
  url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mydb}${DB_SSL_PARAMS:}
```

Locally, `DB_SSL_PARAMS` stays unset (empty string) and the URL is unchanged from a plain
local connection; in production, set `DB_SSL_PARAMS=?sslmode=require` (or whatever your
provider needs — some require `?sslmode=verify-full` plus a CA certificate path) without
touching the base URL pattern or breaking local development for anyone else on the team.

**Framework-specific relaxed binding, worth knowing about:** Spring Boot in particular will
bind environment variables to configuration properties even when there's no explicit
`${VAR}` placeholder for them in your YAML — `SPRING_DATASOURCE_PASSWORD`,
`SPRING_DATA_REDIS_PASSWORD`, and so on are automatically recognized because they map to the
`spring.datasource.password` / `spring.data.redis.password` property paths by Spring's own
relaxed binding rules (upper-snake-case env var ↔ dot-separated lower-case property).
Other frameworks have their own equivalent (dotenv-style tools, Django's environ, etc.) — know
yours, since it means you don't always have to touch the config file at all to add support for
a new deployment-only value.

## 4. Provision the managed database — and get the *reachable* connection details

"Create a database" is the easy part; getting the *right* connection string for your hosting
platform is where time actually goes, because managed Postgres providers (and similar managed
databases) commonly expose **multiple connection modes** that are not interchangeable, and
picking the wrong one produces failures that look unrelated to the actual cause.

### The connection modes, and what each one is actually for

- **Direct connection** — a straight, unpooled connection to the database instance itself.
  Modern managed Postgres providers increasingly make this **IPv6-only** by default (Supabase
  is one current example) unless you pay for a dedicated IPv4 address add-on. If your backend
  host's outbound networking doesn't support IPv6 — common; many container/PaaS platforms
  don't route IPv6 egress — a direct connection will **time out at the TCP level**: a
  `SocketTimeoutException` / "Connect timed out" in the logs, *not* an authentication or
  config-parsing error. This is a networking reachability problem, not a credentials problem
  — don't waste time re-checking passwords or usernames if this is what you're seeing.
- **Transaction-mode connection pooler** (e.g., PgBouncer running in "transaction" mode) —
  reachable over IPv4, but does **not** guarantee the same underlying backend connection is
  held across statements within one client transaction; it hands connections back to the pool
  between transactions, sometimes between statements. ORMs that rely on server-side prepared
  statements being valid for the lifetime they expect (Hibernate is a common one) can break in
  subtle, hard-to-diagnose ways against this mode — sometimes working fine under light load
  and failing intermittently under concurrency, which makes it worse to debug than an outright
  failure.
- **Session-mode connection pooler** — the practical answer when your host can't reach a
  direct/IPv6 connection but your ORM needs connection-stable behavior: IPv4-reachable, *and*
  each client gets a connection held for its whole session (until it disconnects), so it
  behaves like a direct connection as far as your ORM is concerned. If your provider offers
  this mode, prefer it for a traditional backend that already manages its own connection pool
  (HikariCP, c3p0, etc.) — you don't need the provider's pooler *for pooling itself* (your app
  already pools its own connections), you need it specifically for the IPv4 reachability and
  connection-stability guarantees.

| Mode | Reachable over IPv4? | Safe for Hibernate/ORM prepared statements? | When to use |
|---|---|---|---|
| Direct connection | Often not (IPv6-only) | Yes | Your host has confirmed IPv6 egress, or you've paid for a static IPv4 |
| Transaction pooler | Yes | **No** — can break silently | Truly stateless, high-connection-churn clients (e.g. serverless functions), not a long-running backend with its own pool |
| Session pooler | Yes | Yes | The default recommendation for a long-running backend on a host without IPv6 egress |

Also check whether the pooled connection mode changes the **username format** — some providers
suffix a project/tenant identifier onto the username specifically for pooled connections
(`myuser.myprojectref`) while the direct-connection username stays plain (`myuser`). Using the
direct-connection username against a pooler endpoint is a quiet way to get an authentication
failure that looks completely unrelated to the actual cause (you'll often see a generic "bad
password" style error even though the password is correct — it's the *username* format that's
wrong for that endpoint).

### Practical order of operations

1. **Get the credentials from the database provider's "Connection" or "Connection info"
   dashboard page specifically** — not from a general "project overview" or "API" page, which
   often shows a different URL meant for a different purpose (e.g. a REST/GraphQL API gateway
   URL, not a raw Postgres connection string) and can look superficially similar enough to
   paste in by mistake.
2. **Confirm you're copying a bare hostname where a bare hostname is expected.** If your
   config builds the JDBC URL as `jdbc:postgresql://${DB_HOST}:${DB_PORT}/...`, then `DB_HOST`
   must be just `db.example.com`, never `https://db.example.com` — pasting a full URL
   (including scheme) into a field expecting just a host produces a JDBC URL like
   `jdbc:postgresql://https://db.example.com:5432/...`, which the driver rejects immediately
   with an error to the effect of "Driver claims to not accept jdbcUrl."
3. **Try connecting from your actual deployment platform early — not just your laptop.** Your
   laptop's network almost certainly has IPv6 (most modern ISPs and Wi-Fi routers do); many
   PaaS/container platforms' outbound networking doesn't. A connection that works fine when
   you test it locally can fail purely because of *where* it's being made from, not anything
   about the connection string itself. Discovering a networking-mode mismatch before you've
   built five other things on top of a broken assumption is much cheaper than discovering it
   after.
4. **If the very first attempt times out (not errors — actually times out with no response),
   suspect the connection mode/IPv6 issue before anything else.** A wrong password or wrong
   database name produces a fast, explicit rejection from the database server. A silent
   timeout with no response at all is the signature of "this address genuinely isn't
   reachable from here."

## 5. Get schema migrations right on a database that isn't a blank slate

If you're using a migration tool (Flyway, Liquibase, etc.) with a "baseline on first run"
style feature: understand exactly what "non-empty" means to that tool *before* pointing it at
a managed database for the first time. A fresh database from a hosted provider is often **not**
a truly empty schema from the migration tool's point of view — it may already have extensions
registered, default roles, reserved schemas, or provider-specific bookkeeping tables that make
the tool's "does this look pre-existing?" check answer "yes," even though none of your
application's own tables exist yet.

### The dangerous failure mode

The migration tool decides the schema is pre-existing and unmanaged, and instead of running
your migrations from the start, it silently **marks your first migration(s) as already
applied without ever actually running them** (this is literally what "baselining" means: "act
as if everything up to this version is already in place"). Your *second* migration then fails
with confusing errors about tables/columns that don't exist — because the first migration that
was supposed to create them never actually ran. The error message points at migration 2, but
the real problem is migration 1 silently never executing.

This is easy to misread as "my second migration's SQL is broken" when the actual fix has
nothing to do with that migration's contents at all.

### The general fix

Force the "baseline" marker to sit *below* your very first real migration, so baselining (if
it triggers at all) never causes an actual, real migration to be skipped. For Flyway
specifically:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0   # below every real migration (V1, V2, ...) — nothing gets skipped
```

The default `baselineVersion` in Flyway is `1` — which is exactly why this bites people: if
your first real migration is *also* named `V1__...`, a default baseline at version 1 marks
that exact migration as "already applied" without running it. Setting `baseline-version: 0`
sidesteps this because no real migration is ever numbered below 1.

Liquibase's equivalent concept is its changelog lock and `changeLogSync` command, which has
similar "mark as done without running" semantics if misused — the same caution applies:
understand what state your changelog table is in before assuming a fresh run actually applied
anything.

### If you've already hit this

If the migration tool's own bookkeeping table now has a bad baseline record in it (Flyway:
`flyway_schema_history`), you generally need to manually drop that bookkeeping table before
redeploying with the corrected config — fixing the config alone doesn't retroactively undo a
bad baseline that already happened, because the tool will see the (bad) history table already
exists and trust it rather than re-evaluating from scratch.

```sql
DROP TABLE IF EXISTS public.flyway_schema_history;
```

Only do this when you're confident nothing real was actually created by the skipped migration
(check by listing tables in the schema) — if some migrations *did* partially apply before a
failure, dropping just the history table without addressing the partial schema state will
cause the *next* migration attempt to fail differently (trying to create a table that already
exists). When in doubt, and if the data doesn't matter yet (e.g. before go-live), the cleanest
reset is dropping and recreating the entire schema, then letting migrations run fresh:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

## 6. Secrets: generation, storage, and rotation

### Generating secrets correctly

Any signing key, encryption key, or secret the framework turns into cryptographic key material
(JWT signing secrets are the classic case) usually has a **minimum required byte length**, and
libraries frequently derive that length from the **raw byte length of the string value you
provide** — not from some more forgiving interpretation of "seems long enough," and not
necessarily from a base64-decoded length either. A short, hand-typed password-style string
(e.g. a 16–20 character memorable phrase) is a common way to fail this silently at *startup*,
with an error like:

```
The specified key byte array is 128 bits which is not secure enough for any JWT HMAC-SHA
algorithm. ... MUST have a size >= 256 bits
```

Generate secrets properly and paste the *entire* output as the value — don't truncate it to
"look tidier":

```bash
openssl rand -hex 32      # 64 hex characters = 64 bytes = 512 bits
openssl rand -base64 48   # ~64 base64 characters encoding 48 raw bytes = 384 bits
```

Check what your specific library actually measures before assuming a given command's output
is long enough for it — some libraries base64-decode the string you give them before measuring
bit length (so a base64 string encoding 32 raw bytes is "32 bytes" to them), while others
measure the literal string's own byte length as typed/stored (so that same base64 string,
being ~44 characters long, would register as "44 bytes" to them instead). Read the library's
own error message carefully when it tells you how many bits it measured — it's usually enough
to tell you which interpretation it's using, and from there whether your generated value
actually clears the bar.

### Storing secrets

- **Never commit real secret values to git**, including in a `.env` file, even briefly "to
  test something" — git history retains it even after a later commit removes it. Keep a
  `.env.example` with placeholder values (`JWT_SECRET=` or `JWT_SECRET=changeme`) committed
  instead, and actual values only in the hosting platform's environment variable store, a
  local uncommitted `.env` (covered by `.gitignore`), or a dedicated secrets manager for
  larger teams.
- **Set secrets in your hosting platform's dashboard/CLI, not in the Dockerfile or any
  committed config file.** A Dockerfile's `ENV` instruction bakes the value into the image
  layer, which is a much larger exposure surface (anyone who can pull the image can extract
  it) than an environment variable injected at container-start time by the platform.
- **If a secret is ever accidentally committed**, rotating it (generating a new one and
  updating every place that uses it) is the only real fix — removing it from a later commit
  does not remove it from git history, which remains reconstructable.

### Rotation practicalities

Rotating a JWT signing secret invalidates every currently-issued token signed with the old
one — anyone with an active session gets logged out and has to sign in again. This is usually
an acceptable, even desirable, consequence when responding to an actual leak; it's just worth
knowing in advance so it isn't a surprise mid-deployment when you change the secret for an
unrelated reason (e.g. because the first one you generated was too short) and every previously
"logged in" test session stops working.

## 7. Deploy the backend

With the Dockerfile, environment-variable-driven config, a working (reachable, correctly-moded)
database connection string, and a real secret in hand:

1. **Create the web service on your chosen platform**, pointing it at the backend's
   subdirectory (if it's part of a monorepo — set this as the "Root Directory" or equivalent)
   and letting it build from the Dockerfile (most platforms auto-detect a `Dockerfile` at that
   root and offer a "Docker" runtime option).
2. **Set every environment variable the app needs**, cross-referencing your config file from
   §3 line by line — database (host/port/name/user/password/ssl-params), cache (if used), the
   generated secret(s), and anything else referenced. A useful discipline: grep your config
   file for every `${VAR` occurrence and check each one off against what you've actually set.
   ```bash
   grep -oE '\$\{[A-Z_]+' src/main/resources/application.yml | sort -u
   ```
3. **Set a health check path** if the platform supports it (Spring Boot Actuator's
   `/actuator/health` is the standard choice, exposed by adding
   `spring-boot-starter-actuator` and, if you want it reachable without authentication,
   permitting it in your security config) so the platform can tell a genuinely broken deploy
   apart from one that's just still booting, and so it can automatically restart a container
   that becomes unresponsive later.
4. **Watch the first deploy's logs end-to-end** rather than assuming success from "the build
   finished." A Docker image can build perfectly and the *application* can still fail to start
   (bad DB connection, bad secret, migration failure, missing bean) — build success and
   application startup success are two entirely different signals, and platforms report them
   separately (a green "build" step followed by a red/crashed "deploy" step is common and
   means exactly this). The real signal that things are genuinely working is the framework's
   own "started successfully" log line (for Spring Boot: `Started <MainClass> in N seconds`)
   *and* the platform's own "your service is live" confirmation — both, not just one.

### A subtlety worth calling out explicitly: aggregate health checks lie by omission

A health check reporting unhealthy doesn't always mean the *service* is actually broken. If
your health check aggregates multiple components (database, cache, message broker, disk space,
etc. — Spring Boot Actuator's `/health` does exactly this by default, rolling every registered
`HealthIndicator` into one overall status) and one of those components isn't provisioned yet
but also **isn't actually used by any code path yet**, the health check can report `DOWN`
while every real endpoint works perfectly. This happens naturally when a dependency (say,
Redis) is present on the classpath — often added ahead of a feature that will use it later —
but no application code has actually started calling it yet. Don't treat an aggregate health
status as automatically equivalent to "nothing works." Check the health endpoint's detailed
breakdown (Actuator exposes per-component status if you enable
`management.endpoint.health.show-details: always`) to see specifically *which* component is
failing, and separately confirm whether that component is actually load-bearing for the
endpoints you care about before spending time "fixing" something that isn't blocking anything.

## 8. Deploy the frontend(s)

For a Vite (or similar) SPA on a platform like Vercel, Netlify, or Cloudflare Pages:

1. **One project per app**, if you have multiple frontends talking to the same backend (e.g.
   separate patient-facing, staff-facing, and admin apps sharing one API) — each gets its own
   build config, environment variables, and deployed URL, deployed independently.
2. **Point the platform's "root directory" setting at the specific frontend's folder** if it's
   part of a monorepo, so the platform runs `npm install`/`npm run build` from the right
   subdirectory rather than the repo root.
3. **Add a rewrite rule so client-side routing survives a page refresh or a direct link to a
   deep route.** A single-page app served naively will 404 on any path but the exact root,
   because the server has no file at e.g. `/appointments/123` — the router only knows how to
   handle that path *after* `index.html` has loaded and the JS router takes over. Without a
   rewrite rule telling the platform "serve `index.html` for any unmatched path," a refreshed
   or bookmarked deep link breaks:
   ```json
   {
     "framework": "vite",
     "buildCommand": "npm run build",
     "outputDirectory": "dist",
     "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
   }
   ```
4. **Set the frontend's environment variable(s) pointing at the backend's now-live URL** (e.g.
   `VITE_API_BASE_URL=https://your-backend.example.com`), plus anything else the frontend
   needs to function without an authenticated session yet — for instance, a default
   tenant/organization identifier for a multi-tenant app's public, pre-login pages, since
   there's no session yet at that point to carry that context via a header or token claim.
5. **Confirm the production build actually succeeds locally, not just that the dev server
   runs.** This is the single most common late-stage surprise. A dev server (`vite dev`,
   `next dev`, etc.) commonly runs a faster, looser transform that does not fully typecheck
   the code the way a production build's typechecking step does — something can run
   completely fine locally in dev mode and still fail the platform's build outright. The most
   common specific cause: a missing ambient-types declaration file. Vite's `import.meta.env`
   typing, for example, needs a `vite-env.d.ts` file present with
   `/// <reference types="vite/client" />` in it, or TypeScript's standalone build step
   (`tsc -b`, which most Vite+TS templates run as part of `npm run build`) doesn't know that
   property exists on `ImportMeta` and fails the whole build — even though `vite dev` never
   complained, because its transform doesn't perform that check.

   **Run the exact build command the platform will run, locally, before pushing:**
   ```bash
   npm run build
   ```
   This is the cheapest possible way to catch this entire class of bug before burning a
   deploy cycle (and the wait time that comes with it) discovering it on the platform instead.
   Do this for every frontend in a multi-app repo — a bug in one app's build config doesn't
   imply the others are fine.

## 9. Cross-origin access between frontend(s) and backend

Once your frontend(s) live on a different origin (a different domain, including different
subdomains, or a different port during local development, than the backend), the backend needs
CORS configured to allow those origins — otherwise the browser blocks the requests client-side
even though the backend itself would happily answer them if it received the request at all.
This shows up as requests failing in the browser's network tab with a CORS-policy error, often
confusingly *after* the backend has already processed the request (CORS is a browser-enforced,
response-header-based mechanism, not something that prevents the server from doing the work —
it just prevents the browser from letting your JS read the result).

A representative Spring Security CORS configuration:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of(
        "https://your-frontend.vercel.app",
        "http://localhost:5173"   // local dev
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Configure this deliberately — an explicit allow-list of real origins in genuine production
use. A wildcard (`"*"` / `setAllowedOriginPatterns(List.of("*"))`) is a reasonable, low-risk
shortcut to get everything moving during initial setup (especially before you even know your
final frontend URL), but should be tightened to the specific origin(s) that actually need
access before the system is handling real user data — an open CORS policy means *any* website
can make authenticated-looking requests against your API from a logged-in user's browser.

## 10. Observability: logs, health, and knowing it's actually working

Getting a deploy to go green is necessary but not sufficient — verify the thing actually does
what it's supposed to before considering it done:

- **Hit the health endpoint directly** once the platform reports the service live:
  ```bash
  curl -s -w "\nHTTP %{http_code}\n" https://your-backend.example.com/actuator/health
  ```
  Remember §7's caveat: a `DOWN` status here doesn't automatically mean the app is broken —
  check *which* component is failing and whether it's actually used.
- **Exercise a real endpoint that touches the database**, not just the health check, to
  confirm the full path (app → DB) actually works end-to-end, not just that the process is
  running:
  ```bash
  curl -s https://your-backend.example.com/api/v1/some-read-only-endpoint
  ```
- **Watch the live logs during your first real request from the deployed frontend**, not just
  during the deploy itself — most platforms offer a live/streaming log view. This is the
  fastest way to catch a CORS misconfiguration, an auth failure, or a data-shape mismatch that
  a health check alone wouldn't reveal.
- **Test the frontend's actual production build against the actual deployed backend**, not
  your local backend — an app that works in local dev talking to `localhost:8080` can still be
  broken against the deployed backend if an environment variable is missing, misnamed, or
  wasn't picked up because you forgot to redeploy the frontend after changing it (most
  platforms require a fresh build to pick up new environment variable values — changing an env
  var in the dashboard does not retroactively update an already-built static bundle).

## 11. Free-tier and low-cost hosting realities

If you're using free or low-cost tiers of any of these services (very common for a
portfolio/demo project), be aware of behavior that doesn't show up until you've been running
for a while, so it doesn't look like a mysterious regression later:

- **Backend spin-down.** Many free container-hosting tiers stop your service entirely after a
  period of no traffic (commonly ~15 minutes) to conserve shared resources, then have to cold
  start it again (pulling the image, booting the JVM, running migrations) on the next request
  — meaning the first request after a period of inactivity can take many seconds to tens of
  seconds to respond, which looks like the service being broken if you don't expect it.
- **Free database expiration, distinct from spin-down.** Some providers' free-tier *databases*
  (not the backend/web service — the database specifically) are deleted after a fixed window
  of time (e.g. 30 days) regardless of usage, not just paused when idle. This is a hard
  deletion, not a pause — check your specific provider's free-tier terms and either upgrade
  before that window, or budget time to re-provision and re-migrate before it happens if the
  data doesn't need to persist.
- **Compute-scoped vs. always-on database pricing** matters for cost, not just for reliability.
  A provider offering true serverless/scale-to-zero compute typically charges you nothing while
  the database is idle, which is usually cheaper for a low-traffic app — but comes with the
  wake-up latency described above. A provider bundling an always-on compute instance into a
  flat monthly fee avoids that wake-up latency entirely (better for real user-facing traffic)
  but costs the same whether the database is being hit constantly or almost never. Pick based
  on your actual expected traffic pattern, not just the sticker price — a $0 scale-to-zero
  plan under real 24/7 production traffic can end up costing more in metered compute than a
  flat-fee plan would have, once usage stops looking like a demo project's.
- **None of this affects the frontend hosting tiers the same way** — static asset hosting on
  Vercel/Netlify/Cloudflare-style platforms generally doesn't spin down or expire the way a
  backend compute instance or a database does; the free-tier limits there are usually about
  bandwidth/build-minutes instead.

## 12. Redeploying, rolling back, and iterating safely

- **Most platforms auto-redeploy on push to the tracked branch** — know which branch is wired
  up (usually `main`) and treat pushes to it accordingly once the deployment is live; a push
  that would have been harmless pre-deployment (an experimental commit) now triggers a real
  rebuild and restart of a live service.
- **Watch the deploy through to completion before assuming it worked**, every time, not just
  the first time — a later change can reintroduce any of the failure modes above just as
  easily as the first deploy could hit them (a new migration with the same non-empty-schema
  risk, a new environment variable that wasn't set, a new frontend build error).
- **Know your rollback mechanism before you need it.** Most container-hosting platforms let
  you redeploy a previous successful build/image with one click or command — know where that
  control is *before* a bad deploy, not while you're trying to find it during one. Most static
  frontend hosts keep every previous deployment reachable by its own URL and let you
  "promote" any prior one back to the production alias instantly.
- **For a database migration you're unsure about, test it against a copy of production data
  first if the provider supports branching/forking a database** (some managed Postgres
  providers offer this) — cheaper than finding out a migration breaks something only after
  it's run against the real database with no easy undo.

## 13. Security checklist before calling it "production"

A short list of things this guide's process deliberately defers "getting to working" before
"getting right," worth returning to before real users/data are involved:

- [ ] CORS allow-list is the specific real origin(s), not a wildcard (§9)
- [ ] Every secret (JWT signing key, API keys, DB password) is a properly generated random
      value, not a placeholder or dev default left over from local setup (§6)
- [ ] No secret is committed to git history, including in old commits (§6) — if one ever was,
      it's been rotated, not just removed from the latest commit
- [ ] The database user the app connects as has least-privilege access appropriate to what the
      app actually does — not a database superuser/owner role, if your provider lets you scope
      it down
- [ ] TLS/SSL is actually enforced on the database connection (`sslmode=require` or stronger)
      in every non-local environment, not just available
- [ ] Health/actuator endpoints that expose internal details (env values, full stack traces,
      bean listings) are either not exposed publicly or are behind authentication — Spring
      Boot Actuator in particular has several endpoints beyond `/health` that reveal far more
      than you likely want publicly reachable if enabled without thought
      (`management.endpoints.web.exposure.include` should be an explicit, minimal list)
- [ ] Rate limiting or basic abuse protection exists on public-facing write endpoints
      (registration, login, booking-style actions) if the framework/platform doesn't already
      provide it upstream

## 14. Debugging checklist: "it worked locally, it doesn't work deployed"

Roughly the order to check things in, cheapest and most-likely-cause first:

1. **Read the actual deploy log start to finish**, not just the last few lines — the real
   error is often several screens above a downstream symptom (e.g. a web-server startup
   failure log is frequently just the *consequence* of an earlier bean-creation failure that
   scrolled past well before it).
2. **Is the failure a network-level timeout, or an application-level error?** A raw socket/TCP
   timeout points at reachability (wrong host, wrong network mode/IPv6, firewall/security
   group) — no amount of fixing credentials helps that. An application-level exception (auth
   failure, malformed query, missing bean, weak key) points at configuration or code, and
   usually names the specific failing component clearly if you read past the first few lines
   of the stack trace to the innermost "Caused by."
3. **Did every environment variable actually get set, and set to the right *kind* of value?**
   A full URL pasted where a bare hostname was expected, a pooler port used where a direct
   port was expected (or vice versa), a plaintext short secret where a sufficiently-long
   random one was expected — these all produce confusing downstream errors that don't
   obviously point back at the specific env var responsible. Re-derive each value from the
   provider's dashboard from scratch rather than assuming a previously-copied value is still
   correct, if you're stuck.
4. **Is the database schema actually in the state the app expects?** Especially after any
   failed migration attempt — check what tables/history actually exist before assuming a
   config fix alone is enough; a bad baseline or partial migration can leave the schema in a
   state that needs manual cleanup before the corrected config will succeed (§5).
5. **Is the thing reporting "unhealthy" actually load-bearing**, or is it an unused/optional
   dependency (present on the classpath, not yet called by any code) whose health check is
   failing independent of whether the app's real functionality actually works (§7, §10)?
6. **Did the frontend actually rebuild after an environment variable change?** A static
   frontend bundle has its environment variables baked in at build time — changing a value in
   the platform dashboard doesn't retroactively update assets that were already built and
   deployed; it takes effect on the *next* build.
7. **Is this actually a CORS problem disguised as something else?** A request that succeeds
   when tested directly (`curl`, Postman) but fails only from the browser, especially if the
   browser console shows a CORS-policy message, is a CORS configuration issue on the backend
   (§9), not a bug in the request itself.

## 15. Appendix: reference snippets

**Minimal local Docker Compose for development dependencies** (so local dev doesn't require
installing Postgres/Redis natively):

```yaml
services:
  db:
    image: postgres:17
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "5432:5432"
    volumes:
      - db-data:/var/lib/postgresql/data
  redis:
    image: redis:7
    ports:
      - "6379:6379"
volumes:
  db-data:
```

**Grepping a config file for every environment variable it expects**, useful when double-
checking you've set everything on a new platform:

```bash
grep -oE '\$\{[A-Z_]+' src/main/resources/application.yml | sed 's/\${//' | sort -u
```

**Generating a properly sized random secret:**

```bash
openssl rand -hex 32
```

**Checking whether a host is reachable at all, distinct from an application-level error**
(useful for confirming a connection issue is networking, not credentials):

```bash
curl -v --connect-timeout 5 telnet://your-db-host:5432
# or, if you have nc/netcat available:
nc -zv -w 5 your-db-host 5432
```

**Testing the exact frontend production build command locally before pushing:**

```bash
npm run build
```

**Testing a deployed backend's health and a real endpoint:**

```bash
curl -s -w "\nHTTP %{http_code}\n" https://your-backend.example.com/actuator/health
curl -s https://your-backend.example.com/api/v1/some-endpoint
```

---

This manual mirrors the actual sequence of issues resolved deploying this repository's
backend (Spring Boot on Render, containerized) and three frontends (React/Vite on Vercel),
against a Supabase-hosted Postgres database — see `backend/README.md`'s "Deploying to Render"
section for those exact, project-specific values and commands.
