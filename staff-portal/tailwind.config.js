/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#17262B',
        teal: { DEFAULT: '#0E6E66', dark: '#0A5049', light: '#3F8F87' },
        paper: '#F7F8F6',
        panel: '#FFFFFF',
        line: '#DFE4E2',
        amber: '#C97A2B',
        coral: '#C4432B',
      },
      fontFamily: {
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: {
        card: '10px',
      },
    },
  },
  plugins: [],
}
