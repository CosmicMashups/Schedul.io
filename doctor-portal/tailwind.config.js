import { brand } from '../shared/design-tokens/colors.js'
import { radius } from '../shared/design-tokens/radius.js'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#E9EEEC',        // primary text is light — this app runs on a dark clinical slate
        slate: { DEFAULT: '#12191C', panel: '#1A2422', line: '#28332F' },
        // dark-mode expression of the same brand colors patient/staff use in light mode —
        // sourced from shared/design-tokens/colors.js so it can't independently drift again.
        teal: brand.tealDark,
        amber: brand.terracottaDark,
        coral: brand.coralDark,
      },
      fontFamily: {
        display: ['"Source Serif 4"', 'serif'], // used only for the current patient's name — a human touch in a clinical UI
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: radius,
    },
  },
  plugins: [],
}
