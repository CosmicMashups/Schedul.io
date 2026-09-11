# Landing page images

The patient-portal Home page (`src/pages/Home.tsx`) references two photos by exact filename.
Drop the finished files in this directory — the page already has graceful gradient fallbacks, so
nothing breaks before they exist, but here's what the layout expects:

## `hero-consultation.jpg`

Used full-bleed behind the hero section. Text sits in the **bottom-left** on mobile and a
left-to-right gradient scrim covers the left ~60% on desktop, so the image needs a
**doctor/patient consultation moment positioned right-of-center**, with clear space on the left
for the scrim + white text to sit over.

- Orientation: horizontal, roughly 16:9 or wider
- Minimum usable resolution: ~2000×1200

**Prompt used for generation:**
> Documentary-style editorial photograph for a healthcare booking app hero section. A doctor and
> patient in genuine, warm conversation during a consultation in a modern, light-filled clinic
> room — natural window light, soft shadows, candid and unposed, both mid-laugh or attentively
> listening, doctor holding a tablet. Color grade: deep teal (#0E6E66) shadows and warm cream
> highlights, calm and desaturated — NOT a cold clinical blue-white palette, no purple or blue
> "AI gradient" tones anywhere. Composition: horizontal 16:9, subjects positioned right-of-center
> and upper-frame, generous negative space and a soft darker gradient toward the left third of
> the frame for legible white text overlay. Shallow depth of field, 50mm f/1.8 look, subtle film
> grain. No text, no logos, no watermarks. Style reference: Kinfolk magazine meets modern
> healthcare editorial — not stock-photo cheesy.

## `clinic-trust.jpg`

Used as the background of the rounded "trust band" section, with a quote card overlaid on the
left. Needs **negative space on the left/upper-left** for that overlay to sit on.

- Orientation: horizontal, roughly 16:9
- Minimum usable resolution: ~1600×900

**Prompt used for generation:**
> Documentary-style editorial photograph of a warm, welcoming clinic waiting/reception area —
> natural light through large windows, a few healthy plants, warm wood and cream tones with a
> single deep teal (#0E6E66) accent wall or signage element. Either empty of people or with one
> or two people softly out of focus in the background — never posed and facing camera.
> Composition: horizontal 16:9, calm and airy, generous negative space in the left/upper-left
> third for a text overlay, uncluttered. Color grade: warm cream base, deep teal accent, a small
> terracotta (#B5502F) detail if natural (e.g. a chair or plant pot) — no purple/blue "AI
> gradient" tones, no sterile cold-white hospital lighting. No visible logos or text. Style
> reference: high-end boutique clinic editorial photography, Kinfolk/Cereal magazine aesthetic.

## Why only two images, not photos everywhere

`imagegen-frontend-web`'s default is one image per landing section, but this page lives inside a
booking app (not a standalone marketing site) — the "How it works" and "Why choose us" sections
intentionally stay icon/typography-led to keep the page's weight and load time reasonable for
what's ultimately a utility flow, not a campaign page. Both photos share one palette (teal /
cream / terracotta accent) so they read as the same brand world as the rest of the app.
