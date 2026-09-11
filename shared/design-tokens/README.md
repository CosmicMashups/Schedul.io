# Shared design tokens

Single source of truth for what genuinely should be identical across `patient-portal/`,
`staff-portal/`, and `doctor-portal/`. This is a plain-file convention, not a package or
workspace — each portal is an independently deployed Vite app (see root `DEPLOYMENT_GUIDE.md`),
so there's no build-tooling change here, just relative imports:

- `tailwind.config.js` in each portal does `import { brand } from '../shared/design-tokens/colors.js'`
  and `import { radius } from '../shared/design-tokens/radius.js'`.
- `src/styles/index.css` in each portal does `@import '../../../shared/design-tokens/motion.css';`
  near the top.

## What's shared vs. portal-specific

**Shared** (this directory):
- `colors.js` — brand teal/terracotta/coral, each with a light-mode expression (patient, staff)
  and dark-mode expression (doctor) of the *same* color, not two independently-chosen hues.
- `radius.js` — the one `borderRadius.card` value all three portals use.
- `motion.css` — the animation keyframes/utility classes common to all three (`.animate-page`,
  `.animate-scale-in`, `.pulse-ring`, `.skeleton`'s shimmer motion, `.btn-press`, etc.).

**Portal-specific** (stays in each portal's own config/CSS, deliberately not shared):
- Structural/register colors: `ink`, `mist`, `paper`, `line`, `panel`, `slate` — these express each
  portal's distinct light/dark register, not the brand.
- `.focus-ring`'s ring-offset color (background-specific) and `.skeleton`'s background-color/shimmer
  tint (light apps tint dark, the dark app tints light).
- Each portal's signature element (patient's `.pass-stub`, staff's `.now-serving`, staff's
  `.data-table`).
- Typography choices (serif or no serif, which serif) — a deliberate per-portal register decision,
  not drift.

## Why this exists

Before this file existed, doctor-portal's teal (`#2DBDAF`) had silently drifted from patient/staff's
teal (`#0E6E66`) because all three `tailwind.config.js` files hand-typed their own hex values
independently. Import from here instead of re-typing a value, so a future change to the brand color
only has to happen in one place.
