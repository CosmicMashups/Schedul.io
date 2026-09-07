# Schedul.io — Doctor Portal

React + Vite + TypeScript + Tailwind + lucide-react. The third frontend called for in section
50 ("three applications from one backend") — previously the staff console's Queue page stood
in for this; this is the real, separate app.

## Design system
- **Deliberately different register from the other two apps**: a dark clinical slate
  background rather than a light one — this screen is glanced at between patients, often in a
  dim consultation room, and a bright white console competes with that environment.
- **Source Serif 4** is used ONLY for the current/next patient's name — a small human touch
  in an otherwise all-business, single-task screen. Inter for everything else, IBM Plex Mono
  for ticket numbers and times (consistent with the other two apps' type system).
- **This app is intentionally one screen.** No sidebar, no navigation — a doctor's job here is
  "who's with me now, who's next, what does today look like," not browsing. Section 50 lists
  a Doctor Portal with Today's Schedule / Queue / Patient List / Schedule management; this
  MVP scopes to the first two (the daily-use core) and reference-only for the rest (today's
  full schedule renders below the queue focus area).

## What it does
- Resolves the signed-in doctor's own practitioner record (`GET /api/v1/doctors/me` — new
  backend endpoint, mirrors `PatientService.getMe()`'s pattern).
- Shows "Now with you" (the ticket currently SERVING) with a one-tap Complete action.
- Shows "Up next" with Call in / Start visit / Skip — the doctor can pull their own next
  patient rather than waiting on reception, per section 24's "Call Patient" dashboard action.
- Lists today's full appointment schedule below, for reference.

## A documented simplification
A queue ticket is treated as "mine" if it's explicitly assigned to me OR unassigned (common
for walk-ins added without picking a doctor — see the staff console's walk-in form). This is
safe for a single-doctor-per-clinic setup (this demo's data) but would need an explicit
routing step in a genuinely multi-doctor clinic sharing one queue — flagged in `Today.tsx`.

## Linking a doctor's login
A Practitioner record needs `userId` set to a real login for this portal to resolve anything.
The staff console's doctor-creation form doesn't expose this yet (`PractitionerRequest.userId`
is optional and currently only settable via a direct API call) — a small follow-up to wire an
"invite this doctor to the portal" action into `staff-portal/src/pages/Practitioners.tsx`.

## Running

```bash
npm install
cp .env.example .env
npm run dev   # :5175
```
