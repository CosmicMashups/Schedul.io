# Schedul.io — Full Stack (Milestones 1–9 + Doctor Portal)

Four deliverables in this archive:

```
backend/          Spring Boot modular monolith — Milestones 1-4, 7-9 (see backend/README.md)
patient-portal/   React/Vite patient-facing app — Milestone 5 (see patient-portal/README.md)
staff-portal/     React/Vite staff console — Milestone 6, with a calendar grid, staff booking,
                  reschedule, and a Doctor Portal invite flow (see staff-portal/README.md)
doctor-portal/    React/Vite doctor-facing app — the real third app from section 50
                  (see doctor-portal/README.md)
```

Each has its own README with what it covers, its design system, and an honest list of gaps
with the reasoning behind each one. Read those before treating any piece as done.

## What's new in this pass

- **"Invite to Doctor Portal" button** — `staff-portal/Practitioners.tsx` now has an inline
  invite action per doctor. This required a genuinely new backend capability: there was
  previously no way to create ANY login account outside the Flyway dev seed. Two new
  endpoints closed it: `POST /api/v1/staff/users` (general-purpose user creation, not
  Doctor-Portal-specific) and `POST /api/v1/staff/practitioners/{id}/link-user` (links an
  existing account to a practitioner record).
- **Staff-initiated booking** — a "New appointment" form in `staff-portal/Appointments.tsx`
  reuses the exact same booking pipeline the patient portal uses, with `source: PHONE |
  FRONT_DESK` — per section 29's explicit point that staff booking is the same transaction,
  different actor, not a separate code path.
- **Reschedule** — wired into the staff console's appointment list, using the backend's
  existing reschedule endpoint that only the patient portal previously called.

## Running the full stack locally

```bash
cd backend && docker compose up -d && mvn spring-boot:run   # :8080

cd patient-portal && npm install && npm run dev             # :5173
cd staff-portal && npm install && npm run dev                # :5174
cd doctor-portal && npm install && npm run dev               # :5175
```

Sign in to the staff portal first to create a clinic, doctor, service, and schedule rule.
Use the new "Invite to Doctor Portal" button on a doctor's row to create a login you can then
use to sign into the Doctor Portal.

## Deployment

Live architecture: three static frontends on **Vercel**, one containerized backend on
**Render**, one Postgres database on **Supabase**. See [`backend/README.md`](backend/README.md#deploying-to-render)
for the full Render/Supabase walkthrough including gotchas hit getting it running, and
[`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md) for a from-scratch, tool-agnostic version of the
same process written up as a general Java-system deployment manual.

- `backend/Dockerfile` builds the Spring Boot jar (Maven multi-stage build → JRE runtime) —
  point Render's Root Directory at `backend/` and it auto-detects the Dockerfile.
- Each frontend has its own `vercel.json` (Vite framework preset + SPA rewrite for React
  Router) — point Vercel's Root Directory at `patient-portal/`, `staff-portal/`, or
  `doctor-portal/` per project, and set `VITE_API_BASE_URL` to the Render backend's URL.
- `patient-portal` additionally needs `VITE_DEFAULT_TENANT_ID` — its "Find a doctor" page is
  reachable before login, so there's no session yet to carry the tenant.

## What's still open

- No real SMS/email gateway (backend stub; the swap-in point is one method)
- No anonymous "brand new patient, no account yet" self-signup flow
- No drag-to-reschedule directly from the calendar grid (reschedule is a separate action from
  the appointments list, not yet integrated into `Calendar.tsx`'s week/month views)
