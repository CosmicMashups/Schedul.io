# Schedul.io — Patient Portal (Milestone 5)

React + Vite + TypeScript + Tailwind + lucide-react, talking to the Spring Boot backend.

## Design system
- **Palette**: clinical teal primary, cool neutral backgrounds — deliberately not the generic
  cream+terracotta or near-black+neon defaults. `tailwind.config.js` is the source of truth.
- **Type**: Fraunces (display/confirmation moments) + Inter (UI) + IBM Plex Mono (ticket/time
  codes) — see `index.html`'s font links and `tailwind.config.js`'s `fontFamily`.
- **Signature element**: the booking confirmation renders as a torn-edge "boarding pass stub"
  (`.pass-stub` in `styles/index.css`) — ties the digital booking to the physical queue ticket
  the patient will get at the clinic.
- **Motion**: `.animate-page` (route-level fade/slide-in), `.animate-scale-in` (staggered card
  entrance), `.btn-press` (tactile button feedback), `.skeleton` (loading states instead of
  bare "Loading…" text) — all in `styles/index.css`, respecting `prefers-reduced-motion`.
- **Feedback**: a toast system (`components/Toast.tsx`) replaces inline red error text for
  transient success/error feedback; form-level validation errors still render inline.

## Covers (section 56 MVP Patient list)
Registration with consent, doctor search, service selection, availability + slot holding,
booking, my appointments, appointment detail, cancel, reschedule.

## Gaps closed since the last handoff
1. **Doctor/Service clinic references** — `DoctorProfile.tsx` now reads `clinicIds` directly
   off `PractitionerResponse` instead of string-matching a clinic *name*. Backend fix:
   `PractitionerResponse`/`ServiceResponse` now include `clinicIds`/`specialtyIds`.
2. **Returning-patient lookup** — `Login.tsx` now calls the new `GET /api/v1/patients/me`
   after signing in, instead of relying solely on a prior registration's `localStorage` write.

## Gaps still open
1. **No self-serve "brand-new patient with no account" signup** — `POST /api/v1/patients`
   still requires the caller already be authenticated (`SELF_REGISTER` or `PATIENT_WRITE`).
   A true anonymous signup needs a new backend endpoint (create a User + Patient together).
2. **Tenant ID is still a visible manual field** on Login/Register — production would resolve
   a clinic's public URL/subdomain to a tenant id server-side.

## Running

```bash
npm install
cp .env.example .env
npm run dev
```

Fill in `VITE_DEFAULT_TENANT_ID` in `.env` (see the comment in `.env.example`) — the doctor
directory is browsable before login, so there's no session yet to carry `X-Tenant-Id`; this
fills that gap for a single-tenant deployment.

## Deploying (Vercel)

`vercel.json` sets the Vite framework preset and a catch-all rewrite to `index.html` (React
Router needs this so a refreshed/deep-linked route doesn't 404). In the Vercel project:
Root Directory → `patient-portal`; env vars → `VITE_API_BASE_URL` (the backend's URL) and
`VITE_DEFAULT_TENANT_ID`.

`src/vite-env.d.ts` (`/// <reference types="vite/client" />`) has to exist for `tsc -b` to
know `import.meta.env` is valid — `vite dev` doesn't typecheck strictly enough to catch its
absence locally, but Vercel's build (`tsc -b && vite build`) does, and fails without it.

## Additional micro-interactions (final polish pass)
- Shimmer-sweep skeleton loaders (replacing the earlier flat pulse) — `.skeleton` in `styles/index.css`.
- Button spinners (`components/Spinner.tsx`, lucide's `Loader2`) on primary CTAs during in-flight requests, replacing static "Saving…" text with a spinning icon + text.
- A celebratory pulse-ring behind the confirmation/success icon (booking confirmation; check-in success) — `.pulse-ring` in `styles/index.css`.
- Toasts now animate out (not just in) and show a shrinking progress bar for their auto-dismiss countdown — `components/Toast.tsx`.
- Dashboard/report stat numbers count up from 0 on load (`hooks/useCountUp.ts`) instead of popping in fully-formed — implies "live data" without overstating precision.
- A subtle icon "nudge" on hover for nav items (`.icon-nudge`), a small tactile detail rather than a static hover state.
