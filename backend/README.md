# Schedul.io — Backend (Milestones 1–4, 7–8)

Modular monolith for the Clinic Appointment & Patient Flow Management System.

- **Milestone 1 (Foundation)**: multi-tenancy, RBAC, JWT auth, standard API envelope, audit logging.
- **Milestone 2 (Master data)**: Clinic/Location/Room, Practitioner/Specialty, Service catalog, Patient + consent.
- **Milestone 3 (Scheduling engine)**: ScheduleRule → rolling-horizon Slot generation, availability API, slot holding.
- **Milestone 4 (Appointment engine)**: state machine, confirmation policies, booking, cancel/reschedule.
- **Milestone 7 (Check-in + Queue)**: CheckIn, Queue/QueueTicket, doctor-queue call-next/serve/complete.
- **Milestone 8 (Notifications)**: event-driven notification dispatch off the existing audit event bus.

Milestone 9 (reporting/analytics) is not part of this delivery. Frontend apps
(`patient-portal/`, `staff-portal/`) live alongside this directory — see the top-level README.

## Package layout

```
com.clinic.platform
├── tenant, security, identity, audit, common   # Milestone 1 foundation
├── clinic, practitioner, catalog, patient       # Milestone 2 master data
├── scheduling                                   # Milestone 3 scheduling engine
├── appointment                                  # Milestone 4 appointment engine
├── checkin, queue                               # Milestone 7 patient flow
└── notification                                 # Milestone 8 — templates, dispatch, listeners
```

## Milestone 8 notes

- **No real SMS/email gateway is integrated.** `NotificationDispatchService.send()` is an
  explicit stub (logs + records `SENT`) — per section 54's integration-boundary principle,
  building a real gateway call with no test credentials to verify against would be
  unverifiable code. Swapping in Twilio/a local SMS aggregator/SendGrid means changing that
  one method; the template system, event listeners, and `Notification` audit trail are
  already the real interface a gateway integration would sit behind.
- **`AppointmentNotificationListener` and `QueueNotificationListener` subscribe to the same
  `DomainAuditEvent` bus `AuditEventListener` already used for compliance logging** — one
  appointment action now produces both an audit record and, where a template exists, a
  notification. Neither `AppointmentService` nor `QueueService` was touched to add this; they
  already published events for the sole purpose of decoupled subscribers like this (section 42).
- **`NotificationDispatchService.dispatch()` runs in its own `REQUIRES_NEW` transaction**,
  separate from the `AFTER_COMMIT` listener that calls it — a notification failure (bad
  template, a DB hiccup writing the `Notification` row) can never retroactively affect a
  business transaction that has, by definition, already committed.
- **Only a subset of section 30's event list is actually wired**: `APPOINTMENT_REQUESTED`,
  `APPOINTMENT_CONFIRMED`, `APPOINTMENT_REJECTED`, `APPOINTMENT_CANCELLED`,
  `APPOINTMENT_RESCHEDULED`, and `QUEUE_TURN_APPROACHING` (fired on ticket call, not a live
  position countdown — see `QueueNotificationListener`'s javadoc for why). `APPOINTMENT_REMINDER`,
  `DOCTOR_RUNNING_LATE`, `CLINIC_CLOSED`, and `DOCTOR_UNAVAILABLE` exist on the
  `NotificationEvent` enum (so templates can be authored ahead of time) but have no trigger —
  there's no reminder scheduler and no doctor-lateness/absence signal in the codebase yet.
- **A reschedule notification describes the NEW time**, even though the audit event's
  `entityId` points at the ORIGINAL appointment (matching how `AppointmentService.reschedule`
  records it) — `AppointmentNotificationListener` follows `replacementAppointmentId` to fetch
  the new appointment's date/time before rendering.
- **Reused `TENANT_MANAGE` for template authoring and `AUDIT_VIEW` for delivery history**
  rather than adding new permissions — consistent with the same call made for
  `AppointmentTypeController` in Milestone 4, flagged the same way.

## Running locally

```bash
docker compose up -d
mvn spring-boot:run
```

Dev seed data now includes one SMS template per wired event, per tenant (`V7`). Watch the
application log for `[STUB NOTIFICATION]` lines when you confirm/cancel/reschedule an
appointment or call a queue ticket — that's the stub gateway firing.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Database: Supabase (hosted Postgres)

Chosen over Neon for the production path — Supabase's compute is always-on rather than
scale-to-zero, so there's no cold-start latency the first time a patient hits the API after a
period of inactivity. That tradeoff (paying for idle compute) is the right one once real
clinics depend on this, even though it's the more expensive option at low/spiky traffic.

1. Create a project at [supabase.com](https://supabase.com) — free tier (500MB) is enough to
   start; upgrade to Pro ($25/mo, 8GB included) before onboarding a real clinic.
2. In the project dashboard, go to **Project Settings → Database → Connection info**. Use the
   **Session pooler** tab, not Direct connection and not Transaction pooler:
   - Direct connection now resolves to an **IPv6-only address** on most Supabase projects
     (unless you pay for the IPv4 add-on). Render's outbound networking doesn't reliably reach
     IPv6 hosts, so a Direct connection from Render times out at the TCP level
     (`SocketTimeoutException: Connect timed out`) — it's not a config or credentials problem,
     the host just isn't reachable.
   - Transaction pooler (port `6543`) is IPv4-reachable but breaks Hibernate's server-side
     prepared statements, since it doesn't guarantee the same backend connection across
     statements in a transaction.
   - **Session pooler** (port `5432`) is the one that works: IPv4-reachable *and* each client
     gets a session-scoped connection, so it behaves like a direct connection as far as
     Hibernate/HikariCP are concerned.
3. Set these env vars wherever the backend runs (Render, etc.):

   | Env var | Value |
   |---|---|
   | `DB_HOST` | the Session pooler host, e.g. `aws-0-<region>.pooler.supabase.com` |
   | `DB_PORT` | `5432` (Session pooler port — **not** `6543`, that's the Transaction pooler) |
   | `DB_NAME` | `postgres` (Supabase's default database name) |
   | `DB_USER` | `postgres.<project-ref>` — the pooler requires the project ref suffixed onto the username, unlike a direct connection's plain `postgres` |
   | `DB_PASSWORD` | the database password you set when creating the project |
   | `DB_SSL_PARAMS` | `?sslmode=require` — Supabase requires SSL; local dev leaves this unset |

   Flyway runs automatically on startup (`spring.flyway.enabled: true`) and will create all 9
   migrations' worth of schema against the empty Supabase database the first time the app boots.
4. Local dev is unaffected — `DB_SSL_PARAMS` defaults to empty, so `docker compose up -d`
   against local Postgres still works with no SSL params appended.

## What's deliberately NOT here yet

A real SMS/email gateway integration, a reminder scheduler (7-day/24-hour/2-hour, section 30),
doctor-lateness/absence-triggered notifications, and reporting/analytics (Milestone 9).

## Next milestone

**Milestone 9 (Reporting/Analytics)** is the last one named in the original architecture
review's phased plan — access metrics (section 46: third-next-available appointment, average
booking lead time), operational metrics (wait time, doctor utilization), and appointment
quality metrics (no-show/cancellation/confirmation rates) can now be computed since every
underlying event (booking, cancellation, no-show, check-in, queue completion) already has a
durable record in `appointment_status_history`, `audit_logs`, and `queue_tickets`. A reminder
scheduler (extending `SlotHorizonScheduler`'s `@Scheduled` pattern) is the other natural gap
to close before Notifications is genuinely done, if that's a higher priority than reporting.

## Post-Milestone-8 fixes (frontend-driven gap closure)

Building the frontends surfaced real backend gaps; these were fixed in the backend rather
than worked around in the UI:

- **`PractitionerResponse`/`ServiceResponse` now include `clinicIds`/`specialtyIds`**, not just
  display names — frontends no longer need to string-match a name back to an id.
- **New `GET /api/v1/specialties`** (+ `POST` to create) — there was no listing endpoint before.
- **New `GET /api/v1/patients/me`** — resolves the Patient record linked to the calling User.
  `PatientService.register()` now links `Patient.userId` to the caller **only** when they hold
  `SELF_REGISTER` (i.e. registered themselves via the patient portal) — a staff member
  registering a walk-in patient never gets their own account linked to that patient's record.
- **`ReminderScheduler`** (in `notification.service`) closes the `APPOINTMENT_REMINDER`
  trigger gap from Milestone 8 — a single 24-hour-before tier, running hourly, deduped by
  checking for an existing reminder notification per appointment. The 7-day/2-hour tiers from
  section 30 are not implemented — same mechanism, different window/dedupe key, left as a
  follow-up rather than guessed at.
- **New `reporting` module (Milestone 9)** — `GET /api/v1/staff/reports/summary` computes
  section 46's appointment quality metrics (confirmation/cancellation/no-show rate), average
  booking lead time, and average queue wait time over a date range. Computed in Java over a
  bounded query rather than SQL aggregation — appropriate for this deployment scale; flagged
  in the service's javadoc as needing a proper read model at higher volume.

## Post-request additions: Doctor Portal support, reminder tiers

- **`GET /api/v1/doctors/me`** — mirrors `PatientService.getMe()`'s pattern for practitioners.
  Resolves the Practitioner record linked to the calling User's account (`Practitioner.userId`),
  which the new Doctor Portal frontend needs to scope "my schedule"/"my queue". Gated behind
  a new `DOCTOR_PORTAL_ACCESS` permission and a seeded `DOCTOR` role (`V9` migration).
- **`PractitionerRequest.userId`** (optional) — staff can now link a doctor's portal login when
  creating/updating a Practitioner record. Not yet wired into the staff console's UI (flagged
  in `doctor-portal/README.md`) — currently only settable via a direct API call.
- **Reminder tiers expanded from one to three** — `NotificationEvent.APPOINTMENT_REMINDER` (a
  single value) is now three values: `APPOINTMENT_REMINDER_7_DAY`, `_24_HOUR`, `_2_HOUR`.
  Giving each tier its own enum value (rather than one event plus a separate "tier" column)
  means `ReminderScheduler`'s existing per-appointment dedupe check works correctly per tier
  with no schema change — the event code itself IS the tier. `V9` cleans up the old
  single-tier templates (Flyway migrations are immutable, so this couldn't be done by editing
  `V7`/`V8`) and reseeds all three.

## Post-request: user management + invite flow

- **New `identity` endpoints**: `POST /api/v1/staff/users` (create a login account with a set
  of role codes) and `GET /api/v1/staff/users?role=` — closes the gap that there was
  previously no way to create a staff/doctor account outside the Flyway dev seed. Guarded by
  the existing `USER_MANAGE` permission.
- **New `POST /api/v1/staff/practitioners/{id}/link-user`** — a lightweight endpoint that only
  sets `Practitioner.userId`, separate from the full `PractitionerRequest` update (which
  requires specialties/clinics) since linking a login is an unrelated concern. Together with
  the new user-creation endpoint, this is what the staff console's "Invite to Doctor Portal"
  button calls: create the account, then link it.
