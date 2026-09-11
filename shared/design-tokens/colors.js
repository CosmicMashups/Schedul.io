/**
 * Single source of truth for Schedul.io's brand colors, shared by all three portals'
 * tailwind.config.js. Each brand color has a light-mode expression (used by patient-portal
 * and staff-portal, which are both light-register apps) and a dark-mode expression (used by
 * doctor-portal, which runs on a dark clinical slate). These are the SAME brand color, tuned
 * for contrast against opposite backgrounds — not two independently-chosen hues that happen to
 * share a name. Previously doctor-portal's teal (#2DBDAF) drifted from patient/staff's teal
 * (#0E6E66) because each tailwind.config.js hand-typed its own hex; importing from here is what
 * prevents that drift from recurring.
 */

export const brand = {
  teal: { DEFAULT: '#0B5E56', dark: '#073F3A', light: '#2F8B81' },
  tealDark: { DEFAULT: '#34C2B3', dark: '#1B8F82', light: '#6BDDCE' },

  terracotta: { DEFAULT: '#B5502F', dark: '#8A3B21', light: '#D9825F' },
  terracottaDark: { DEFAULT: '#E2925F', dark: '#C97540', light: '#F0B58A' },

  coral: { DEFAULT: '#B5402A', dark: '#8A2E1C', light: '#D97159' },
  coralDark: { DEFAULT: '#E2685A', dark: '#C24F42', light: '#F0958A' },
}
