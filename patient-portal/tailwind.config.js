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
        mist: '#EDF3F0',
        paper: '#FBFCFA',
        line: '#DAE2DC',
        amber: brand.terracotta,
        coral: brand.coral,
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        sans: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      borderRadius: radius,
    },
  },
  plugins: [],
}
