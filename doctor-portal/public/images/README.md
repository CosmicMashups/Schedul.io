# Login page image

The doctor portal's Login page (`src/pages/Login.tsx`) references one photo by exact filename.
Drop it in this directory — the page has a graceful on-brand dark gradient fallback, so nothing
breaks before it exists.

## `login-consult-room.jpg`

Used full-bleed behind the sign-in screen, with the form card floating on the **right** on
desktop (centered on mobile). Needs clear negative space on the right ~40% of the frame.

- Orientation: horizontal, roughly 16:9 or wider
- Minimum usable resolution: ~2000×1200

**Prompt used for generation:**
> Documentary-style editorial photograph of a quiet, dim clinical consultation room at dusk — a
> single warm desk lamp or low window light as the main light source, a stethoscope resting on a
> desk or a white coat on a hook, completely empty of people, still and calm. Color grade: deep
> dark teal-to-ink gradient (#12191C toward a #1F9184 teal accent glow), warm low-key lighting —
> NOT cold blue-white clinical lighting, no purple/blue "AI gradient" tones. Composition:
> horizontal 16:9, moody and quiet, subject positioned left-of-center, generous negative space
> and a darker gradient toward the right third of the frame for a floating card to sit over.
> Cinematic, shallow depth of field, subtle grain. No text, no logos, no people, no watermarks.
> Style: night-shift mood — evokes "glanced at between patients in a dim room," matching the
> app's own dark, glanceable identity (same as `doctor-portal`'s existing dark slate palette).

## Why the login screen only, not a full marketing page

Doctor portal is an internal, login-gated single-screen tool with no public visitors — there's no
landing page for it to have. This applies the same art-direction rigor (typography, one generated
image, GSAP entrance) to the one screen that actually functions as its "front door," without
adding scroll sections, navigation, or marketing copy that would contradict the app's own
deliberately distraction-free, single-glance design.
