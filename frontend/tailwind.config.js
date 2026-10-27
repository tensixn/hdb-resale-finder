/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
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
          50: '#f6f7f9',
          100: '#eceef2',
          200: '#d5d9e2',
          300: '#b1b8c7',
          400: '#8791a7',
          500: '#68738c',
          600: '#535c73',
          700: '#444b5e',
          800: '#3b4150',
          900: '#2a2e38',
          950: '#171a21',
        },
      },
      boxShadow: {
        card: '0 1px 2px rgba(23,26,33,0.04), 0 4px 16px rgba(23,26,33,0.06)',
        pop: '0 8px 30px rgba(23,26,33,0.14)',
      },
    },
  },
  plugins: [],
};
