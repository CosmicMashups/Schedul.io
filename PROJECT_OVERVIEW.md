# Schedul.io — Clinic Access Platform

> Resume/portfolio reference doc. Summarizes the system's purpose, architecture, and feature
> set so it can be accurately described in a resume, cover letter, or interview — not a
> replacement for the technical READMEs in each subproject.

## Overview

A multi-tenant SaaS platform for clinics to manage the full patient-access lifecycle:
appointment booking, front-desk check-in, doctor queueing, and patient communication —
delivered as one backend and three role-specific frontends (patient, staff, doctor). Modeled
as a phased, milestone-driven build (9 milestones) covering everything from schema/auth
foundations up through scheduling, booking, queue management, notifications, and reporting.

**Problem it solves:** replaces phone-call-based clinic scheduling with self-service booking,
gives front-desk staff a single console for walk-ins/phone bookings/check-in/queue control,
and gives doctors a focused view of their own day without exposing the rest of the clinic's
admin surface.

## Architecture

**Style:** Modular monolith backend + independently deployable single-page frontends,
communicating over a versioned REST API.

```
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ Patient Portal   │   │  Staff Console   │   │  Doctor Portal   │
│ (React/Vite SPA) │   │ (React/Vite SPA) │   │ (React/Vite SPA) │
└────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘
         │                      │                      │
         └──────────────┬───────┴──────────────────────┘
                         │  REST + JWT (X-Tenant-Id header)
                ┌────────▼─────────┐
                │  Spring Boot API  │   modular monolith, package-by-domain
                │  (backend)        │
                └────────┬─────────┘
                         │
        ┌────────────────┼─────────────────┐
        ▼                ▼                 ▼
   PostgreSQL         Redis              (Kafka wired,
   (system of         (cache/session      event backbone
   record)            layer)              for async work)
```

**Backend — package-by-domain, not layer-by-layer:**

```
com.clinic.platform
├── tenant, security, identity, audit, common   # multi-tenancy, auth, RBAC, audit trail
├── clinic, practitioner, catalog, patient       # master data
├── scheduling                                   # availability/slot engine
├── appointment                                  # booking state machine
├── checkin, queue                               # front-desk + doctor queue flow
├── notification                                 # templated, event-driven messaging
└── reporting                                    # operational/quality metrics
```

Each domain owns its controller/service/repository/domain/DTO stack; cross-domain
communication happens through an internal **domain event bus** (`DomainAuditEvent`), not
direct service-to-service calls — the same event that produces an audit-log entry is what a
notification listener (or, later, a reporting read-model) subscribes to. This keeps
Appointment/Queue services unaware that anything downstream is listening.

**Key architectural decisions:**
- **Multi-tenancy via request-scoped tenant context**, resolved from an `X-Tenant-Id` header
  by a servlet filter that runs before Spring Security, so JWT validation can cross-check the
  token's tenant claim against it. Every tenant-scoped repository query and entity listener
  enforces isolation at the persistence layer, not just in controllers.
- **Row-level pessimistic locking for slot booking** (`SELECT ... FOR UPDATE`) — the one place
  correctness under concurrent requests matters more than throughput (two patients can't book
  the same slot).
- **Event-driven notifications** built on the existing audit-event bus rather than a new
  pub/sub path — zero changes needed to the services that originate the events.
- **Explicit integration boundary for third-party gateways**: the SMS/email dispatch service is
  a well-defined interface with a stub implementation, isolating "code I can verify" from "an
  external call I have no test credentials for."
- **Flyway-versioned schema**, immutable migrations — schema evolves forward-only, including a
  migration written specifically to retire and reseed data from an earlier migration rather
  than editing it in place.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot (Web, Security, Data JPA, Validation, Actuator), Hibernate |
| Auth | JWT (`jjwt`), Spring Security, role/permission-based authorization |
| Database | PostgreSQL, Flyway migrations |
| Caching/session | Redis |
| Messaging | Kafka (event backbone) |
| API docs | springdoc-openapi (Swagger UI) |
| Mapping/boilerplate | MapStruct, Lombok |
| Frontend (×3 apps) | React 18, TypeScript, Vite, React Router, Tailwind CSS |
| Build | Maven (backend), npm/Vite (frontends) |

## Features

### Foundation
- Multi-tenant data isolation with tenant-aware repositories and entity listeners
- JWT-based authentication and role/permission-based authorization (RBAC)
- Standardized API response envelope and centralized exception handling
- Full audit logging of domain events

### Master data
- Clinic / location / room management
- Practitioner and specialty management
- Service catalog (procedures, durations, fees)
- Patient records with consent tracking

### Scheduling engine
- Rule-based recurring availability → rolling-horizon slot generation
- Real-time availability search API
- Slot holding (temporary reservation) with concurrency-safe row locking to prevent double-booking

### Appointment engine
- Full appointment state machine (requested → confirmed → completed / cancelled / no-show)
- Configurable confirmation policies
- Booking, cancellation, and reschedule flows shared identically across patient self-booking
  and staff-assisted (phone/front-desk) booking — one pipeline, tagged by source

### Patient flow (check-in & queue)
- Front-desk check-in against a booked appointment
- Per-clinic doctor queue with call-next / serve / complete actions
- Real-time queue-position-driven notifications

### Notifications
- Template-driven, event-triggered messaging (confirmation, rejection, cancellation,
  reschedule, queue-turn-approaching)
- Tiered appointment reminders (7-day / 24-hour / 2-hour) via a scheduled dedupe-aware job
- Swappable gateway boundary (stubbed for SMS/email; one method to wire a real provider)
- Notifications run in an isolated transaction so a delivery failure can never roll back the
  business transaction that triggered it

### Reporting & analytics
- Appointment quality metrics: confirmation rate, cancellation rate, no-show rate
- Operational metrics: average booking lead time, average queue wait time
- Date-range-scoped summary endpoint powering the staff dashboard

### Identity & access management
- Staff-driven user provisioning (create login accounts with role assignments) — no longer
  limited to seed data
- "Invite to Doctor Portal" flow: create an account and link it to a practitioner record in
  one guided action from the staff console
- Self-registration vs. staff-registration distinction, so a staff member registering a
  walk-in patient doesn't accidentally link that patient to their own account

### Three role-scoped applications
- **Patient Portal** — public doctor directory search, appointment booking, self-service
  cancel/reschedule, appointment history
- **Staff Console** — dashboard, calendar (week/month grid), appointment management,
  check-in, queue control, patient/practitioner/service/schedule administration, reports
- **Doctor Portal** — scoped to "my schedule" and "my queue" via the calling user's linked
  practitioner record, without exposing clinic-wide administration

## Engineering practices demonstrated

- Multi-tenant SaaS architecture with defense-in-depth tenant isolation
- Domain-driven, package-by-feature modular monolith design
- Event-driven internal architecture (audit log and notifications as subscribers to the same
  event stream, not bolted-on side effects)
- Concurrency-safe booking logic under simultaneous requests
- Incremental, versioned database schema evolution (Flyway) across 9 migrations
- RBAC and multi-actor authorization design (patient vs. staff vs. doctor permission sets)
- Deliberate, documented scope boundaries — explicit "what's stubbed and why" rather than
  fake/mocked integrations presented as done
- Full-stack ownership: relational schema → Spring Boot API → three independent React SPAs
