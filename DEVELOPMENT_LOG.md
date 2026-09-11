# Schedul.io — Development History

> Portfolio/resume reference. Tells the story of how the system was built — phases, key
> decisions, and problems solved — so it can be summarized accurately in a resume bullet,
> cover letter, or interview. For the current architecture and feature set, see
> [`PROJECT_OVERVIEW.md`](PROJECT_OVERVIEW.md). For local setup, see [`README.md`](README.md).

## What it is

Schedul.io is a multi-tenant SaaS platform that replaces phone-call-based clinic scheduling
with self-service booking, front-desk workflow tools, and a doctor-facing queue view. It's a
full-stack build: one Spring Boot backend and three independently deployable React SPAs
(patient, staff, doctor), built and shipped end to end — schema design through production
deployment.

## Build approach: milestone-driven

The system was built as nine sequential milestones, each shipping a working, demoable slice
rather than building all layers in parallel. This kept every milestone independently
verifiable and meant the backend's contracts were proven by real frontend consumers before
the next domain was layered on.

1. **Foundation** — multi-tenant schema, JWT auth, RBAC, audit logging, standardized API
   response envelope and exception handling.
2. **Master data** — clinics/locations/rooms, practitioners and specialties, service catalog,
   patient records with consent tracking.
3. **Scheduling engine** — rule-based recurring availability, rolling-horizon slot generation,
   real-time availability search.
4. **Appointment engine** — full booking state machine (requested → confirmed → completed /
   cancelled / no-show), concurrency-safe slot holding.
5. **Patient Portal** (React/Vite) — public doctor search, booking, self-service
   cancel/reschedule, appointment history.
6. **Staff Console** (React/Vite) — dashboard, week/month calendar grid, staff-assisted
   booking, check-in, queue control, and admin screens for patients/practitioners/services/
   schedules, plus reporting.
7. **Check-in & queue** — front-desk check-in against a booked appointment, per-clinic doctor
   queue with call-next/serve/complete actions, real-time queue-position notifications.
8. **Notifications** — template-driven, event-triggered messaging (confirmation, rejection,
   cancellation, reschedule, queue-turn-approaching) plus tiered reminder scheduling
   (7-day / 24-hour / 2-hour).
9. **Reporting & identity** — operational/quality metrics (confirmation rate, cancellation
   rate, no-show rate, average lead time, average queue wait), staff-driven user
   provisioning, and an "Invite to Doctor Portal" flow.

A fourth frontend, the **Doctor Portal**, was added as a follow-on pass after milestone 9,
scoped to "my schedule" and "my queue" via the signed-in user's linked practitioner record —
deliberately excluding clinic-wide administration.

## Architectural decisions and why

- **Package-by-domain, not layer-by-layer backend.** Each domain (tenant, security, clinic,
  practitioner, catalog, patient, scheduling, appointment, checkin, queue, notification,
  reporting) owns its full controller → service → repository → domain → DTO stack. Chosen so
  a domain's code stays cohesive and swappable rather than scattered across `controllers/`,
  `services/`, `repositories/` folders shared by everything.

- **Cross-domain communication via a domain event bus, not direct service calls.** The same
  `DomainAuditEvent` that produces an audit-log entry is what the notification listener (and
  later a reporting read-model) subscribes to. Appointment and Queue services stay unaware
  that anything downstream is listening — adding notifications required zero changes to the
  services that originate the events.

- **Multi-tenancy via request-scoped tenant context.** An `X-Tenant-Id` header is resolved by
  a servlet filter running *before* Spring Security, so JWT validation can cross-check the
  token's tenant claim against it. Isolation is enforced at the persistence layer (tenant-aware
  repositories and entity listeners), not just at the controller boundary — a deliberate
  defense-in-depth choice for a multi-tenant system where a leaked query is a data breach, not
  a bug.

- **Row-level pessimistic locking for slot booking** (`SELECT ... FOR UPDATE`). The one place
  in the system where correctness under concurrent requests matters more than throughput: two
  patients racing for the same slot must not both win.

- **Staff-assisted booking reuses the patient-booking pipeline**, tagged by `source: PHONE |
  FRONT_DESK` rather than being a parallel code path. One booking transaction, multiple
  entry points — avoids the classic drift where the "staff version" of a flow silently
  diverges from the self-service version.

- **Notifications run in an isolated transaction**, so a delivery failure can never roll back
  the business transaction that triggered it. Paired with a **stubbed SMS/email gateway**
  behind a single well-defined interface — an explicit, documented boundary between "code I
  can verify" and "an external call with no test credentials," rather than a fake integration
  presented as done.

- **Flyway-versioned, immutable schema migrations.** Schema evolves forward-only; one
  migration was written specifically to retire and reseed data from an earlier migration
  rather than editing history in place.

## Problems hit and fixed

- **Flyway silently skipping `V1__init` on managed Postgres.** Managed providers (Supabase)
  pre-create schema/roles that made Flyway treat the baseline migration as already applied.
  Fixed by making the baseline behavior explicit rather than relying on Flyway's default
  detection.
- **Supabase connection mode mismatch.** Initial guidance pointed at the wrong pooler mode;
  corrected to the Session pooler, which Flyway's DDL migrations require (the Transaction
  pooler doesn't support the session-level features Flyway needs).
- **SSL handshake against a hosted Postgres instance.** Added an optional SSL parameter to the
  datasource config so the same codebase runs unmodified against local Docker Postgres (no
  TLS) and Supabase (requires TLS).
- **Null-pointer risk in the doctor portal's queue "call next" action** — guarded `queueId`
  before invoking `callNext` so an empty queue state can't throw.
- **Missing Vite env wiring** on one of the frontends, causing the built app to silently fall
  back to a default API base URL instead of the configured backend.

## Deployment

Shipped as a real three-tier deployment rather than left as a local-only demo:

- **Frontends** — each of the three (later four) SPAs deployed independently to **Vercel**,
  each with its own `vercel.json` (Vite preset + SPA rewrite for React Router) and its own
  `VITE_API_BASE_URL` pointing at the backend.
- **Backend** — containerized via a multi-stage `Dockerfile` (Maven build stage → JRE runtime
  stage) and deployed to **Render**, which auto-detects the Dockerfile when Root Directory is
  pointed at `backend/`.
- **Database** — **Supabase**-hosted Postgres, reached via the connection-pooling and
  SSL fixes above.

The full process — including the gotchas above — was written up twice: once as a
project-specific walkthrough in `backend/README.md`, and once generalized into a
tool-agnostic Java-deployment manual (`DEPLOYMENT_GUIDE.md`) reusable for other projects.

## Deliberately out of scope

Documented rather than silently missing, so gaps read as scoping decisions instead of bugs:

- No real SMS/email gateway — stubbed behind a swappable interface.
- No anonymous self-signup for brand-new patients (staff-provisioned accounts only, plus
  patient self-registration through the existing flow).
- No drag-to-reschedule directly from the calendar grid — reschedule exists as a separate
  action, not yet integrated into the week/month calendar view.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot (Web, Security, Data JPA, Validation, Actuator), Hibernate |
| Auth | JWT (`jjwt`), Spring Security, role/permission-based authorization |
| Database | PostgreSQL, Flyway migrations |
| Caching/session | Redis |
| Messaging | Kafka (event backbone) |
| API docs | springdoc-openapi (Swagger UI) |
| Mapping/boilerplate | MapStruct, Lombok |
| Frontend (×4 apps) | React 18, TypeScript, Vite, React Router, Tailwind CSS |
| Build | Maven (backend), npm/Vite (frontends) |
| Infra | Docker, Vercel (frontends), Render (backend), Supabase (Postgres) |

## Skills this demonstrates

- Full-stack ownership: relational schema → API → three-to-four independent SPAs → production
  deployment.
- Multi-tenant SaaS architecture with defense-in-depth isolation.
- Domain-driven, package-by-feature modular monolith design.
- Event-driven internal architecture (audit log and notifications as subscribers to one event
  stream, not bolted-on side effects).
- Concurrency-safe booking logic under simultaneous requests.
- Incremental, versioned schema evolution across 9+ Flyway migrations.
- RBAC and multi-actor authorization design (patient vs. staff vs. doctor permission sets).
- Debugging real managed-infrastructure issues (Flyway baseline detection, connection pooler
  mode, TLS) rather than only local-Docker development.
- Deliberate, documented scope boundaries instead of unfinished work presented as complete.
