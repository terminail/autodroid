/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{html,js,svelte,ts}'],
  theme: {
    extend: {},
  },
  plugins: [],
  // 禁用CSS优化，确保包含所有样式
  experimental: {
    optimizeUniversalDefaults: false
  },
  // 确保包含所有颜色样式
  safelist: [
    'text-blue-500',
    'bg-blue-500', 
    'text-gray-500',
    'bg-gray-500',
    'text-red-500',
    'bg-red-500',
    'text-green-500',
    'bg-green-500',
    'text-yellow-500',
    'bg-yellow-500',
    'text-purple-500',
    'bg-purple-500',
    'text-indigo-500',
    'bg-indigo-500',
    'text-pink-500',
    'bg-pink-500',
    'text-orange-500',
    'bg-orange-500',
    'divide-y',
    'divide-gray-200',
    'border-gray-200'
  ]
}