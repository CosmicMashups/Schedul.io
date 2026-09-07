/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#E8EDEC',        // primary text is light — this app runs on a dark clinical slate
        slate: { DEFAULT: '#12191C', panel: '#1A2327', line: '#293338' },
        teal: { DEFAULT: '#2DBDAF', dark: '#1F9184', light: '#63D4C8' },
        amber: '#E0A458',
        coral: '#E2685A',
      },
      fontFamily: {
        display: ['"Source Serif 4"', 'serif'], // used only for the current patient's name — a human touch in a clinical UI
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: { card: '14px' },
    },
  },
  plugins: [],
}
