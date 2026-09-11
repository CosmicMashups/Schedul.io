import { brand } from '../shared/design-tokens/colors.js'
import { radius } from '../shared/design-tokens/radius.js'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#1B2724',
        teal: brand.teal,
        paper: '#F6F7F4',
        panel: '#FFFFFF',
        line: '#E1E6DF',
        amber: brand.terracotta,
        coral: brand.coral,
      },
      fontFamily: {
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: radius,
    },
  },
  plugins: [],
}
