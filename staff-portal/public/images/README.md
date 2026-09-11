# Login page image

The staff console's Login page (`src/pages/Login.tsx`) references one photo by exact filename.
Drop it in this directory — the page has a graceful on-brand gradient fallback, so nothing breaks
before it exists.

## `login-desk.jpg`

Used full-bleed behind the sign-in screen, with the form card floating on the **right** on
desktop (left on mobile, stacked above). Needs clear negative space on the right ~40% of the
frame for the card + its scrim to sit over.

- Orientation: horizontal, roughly 16:9 or wider
- Minimum usable resolution: ~2000×1200

**Prompt used for generation:**
> Documentary-style editorial photograph of a modern clinic front-desk / reception workspace,
> shot from behind the desk — a monitor showing a soft-focus calendar UI, warm afternoon light
> through a window, tidy and calm, no visible readable text on any screen. Either empty of
> people, or a single person's hands typing, out of focus. Color grade: warm cream (#F6F7F4)
> base with deep teal (#0E6E66) accent details — a folder, a desk plant, signage — calm and
> desaturated, NOT a cold clinical blue-white palette, no purple/blue "AI gradient" tones
> anywhere. Composition: horizontal 16:9, subject/desk positioned left-of-center, generous
> negative space and a soft darker gradient toward the right third of the frame for a floating
> white sign-in card to sit over. Shallow depth of field, 35mm look, subtle film grain. No text,
> no logos, no watermarks. Style reference: the same Kinfolk/Cereal-magazine editorial calm as
> the patient-facing app, but from an operations/front-desk vantage rather than a patient-facing
> one.

## Why the login screen only, not a full marketing page

Staff console is an internal, login-gated ops tool with no public visitors — there's no landing
page for it to have. This applies the same art-direction rigor (typography, one generated image,
GSAP entrance) to the one screen that actually functions as its "front door," without inventing
scroll sections or marketing copy that don't belong in a daily-use internal tool.
