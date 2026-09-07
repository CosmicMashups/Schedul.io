# Schedul.io — Staff Console (Milestone 6)

React + Vite + TypeScript + Tailwind + lucide-react, talking to the Spring Boot backend.

## Design system
- **Palette**: same clinical teal brand as the patient portal, but a flatter, denser
  "operations console" register — no serif, tighter type scale, `.panel`/`.data-table`
  utilities for information-dense layouts.
- **Signature element**: the queue page's "Now Serving" marquee (`.now-serving` gradient
  panel) — giant mono ticket number, mission-control feel, the doctor-station focal point.
- **Motion**: `.animate-page`, `.animate-scale-in`, `.animate-count-pop` (stat cards pop on
  load), `.btn-press`, `.skeleton` — same system as the patient portal for visual consistency
  across both apps.
- **Feedback**: toast system for every mutation (confirm/reject/cancel/check-in/queue
  actions) instead of inline error text; empty states use icon + message + a clear next
  action rather than a bare "No results."

## Covers (section 56 MVP Staff list)
Dashboard (section 25's reception command center), appointment search + confirm/reject/
cancel/no-show, check-in, live queue board with walk-in support, patient search + quick
registration + notification history, doctor/service/schedule management, reports (section 46).

## Gaps closed since the last handoff
1. **Specialty picker** — `Practitioners.tsx` now uses `GET /api/v1/specialties` (new backend
   endpoint) with a real multi-select instead of a raw UUID text field.
2. **Check-in click-through** — the appointments list's "Check in" action now navigates to
   `/check-in` with the appointment id pre-filled via router state, instead of requiring a
   manual paste.
3. **Walk-in queueing UI** — `Queue.tsx` now has an inline "Walk-in" form (patient search +
   add to queue), wired to the backend's existing `POST /queues/{id}/walk-in`.
4. **Notification history** — `Patients.tsx` now has a "Notifications" action per search
   result, showing delivery history via the new `GET /api/v1/staff/patients/{id}/notifications`.
5. **Dashboard + Reports** — new pages wired to the new `GET /api/v1/staff/reports/summary`
   backend endpoint (Milestone 9).

## Gaps closed in this pass
6. **Calendar grid view** (section 51) — new `Calendar.tsx` with Week and Month grids
   (day view is covered by the existing `Appointments.tsx` list, so not duplicated). Week
   view positions appointments by actual time/duration; Month view shows per-day chips with
   an overflow count and clicking a day jumps into that week. Filterable by clinic/doctor,
   color-coded by status using the same palette as `StatusPill`.
7. **Separate Doctor Portal** (section 50) — a real third app now exists at `../doctor-portal`
   instead of `Queue.tsx` standing in for it. See its own README for what it covers.

## Gaps closed in this pass
8. **"Invite to Doctor Portal" button** — `Practitioners.tsx` now has an inline invite form per
   doctor: creates a login account (new backend `POST /api/v1/staff/users` with
   `roleCodes: ['DOCTOR']`) and links it (new `POST /staff/practitioners/{id}/link-user`) in
   two calls. There was previously no way to create ANY staff login outside the Flyway dev
   seed — this is a general-purpose fix, not Doctor-Portal-specific.
9. **Staff-initiated booking** — `Appointments.tsx` now has a "New appointment" form: search
   patient, pick doctor/service, browse availability, hold a slot, book with
   `source: PHONE | FRONT_DESK` — reusing the exact same booking pipeline the patient portal
   uses (section 29's explicit point: staff booking should be the same transaction, different
   actor).
10. **Reschedule** — a "Reschedule" action on `CONFIRMED` appointments opens an inline slot
    picker for the same doctor/clinic and calls the existing reschedule endpoint.

## Gaps still open
None specific to this app beyond what's listed at the top-level delivery README (no real
SMS gateway, no anonymous patient signup, no calendar-view drag-to-reschedule).

## Running

```bash
npm install
cp .env.example .env
npm run dev
```

## Additional micro-interactions (final polish pass)
- Shimmer-sweep skeleton loaders (replacing the earlier flat pulse) — `.skeleton` in `styles/index.css`.
- Button spinners (`components/Spinner.tsx`, lucide's `Loader2`) on primary CTAs during in-flight requests, replacing static "Saving…" text with a spinning icon + text.
- A celebratory pulse-ring behind the confirmation/success icon (booking confirmation; check-in success) — `.pulse-ring` in `styles/index.css`.
- Toasts now animate out (not just in) and show a shrinking progress bar for their auto-dismiss countdown — `components/Toast.tsx`.
- Dashboard/report stat numbers count up from 0 on load (`hooks/useCountUp.ts`) instead of popping in fully-formed — implies "live data" without overstating precision.
- A subtle icon "nudge" on hover for nav items (`.icon-nudge`), a small tactile detail rather than a static hover state.
