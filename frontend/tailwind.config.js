/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Geist', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['Fraunces', 'Georgia', 'serif'],
      },
      colors: {
        brand: {
          50: '#effaf7',
          100: '#d7f2ea',
          200: '#b0e4d6',
          300: '#7fcfbd',
          400: '#4bb39f',
          500: '#2a9784',
          600: '#1d7a6b',
          700: '#1a6258',
          800: '#184f48',
          900: '#16423c',
          950: '#0a2622',
        },
        ink: {
          50: 'rgb(var(--ink-50) / <alpha-value>)',
          100: 'rgb(var(--ink-100) / <alpha-value>)',
          200: 'rgb(var(--ink-200) / <alpha-value>)',
          300: 'rgb(var(--ink-300) / <alpha-value>)',
          400: 'rgb(var(--ink-400) / <alpha-value>)',
          500: 'rgb(var(--ink-500) / <alpha-value>)',
          600: 'rgb(var(--ink-600) / <alpha-value>)',
          700: 'rgb(var(--ink-700) / <alpha-value>)',
          800: 'rgb(var(--ink-800) / <alpha-value>)',
          900: 'rgb(var(--ink-900) / <alpha-value>)',
          950: 'rgb(var(--ink-950) / <alpha-value>)',
        },
      },
      boxShadow: {
        card: '0 1px 2px rgba(22,50,46,0.05), 0 4px 16px rgba(22,50,46,0.07)',
        pop: '0 8px 30px rgba(22,50,46,0.16)',
      },
    },
  },
  plugins: [],
};
