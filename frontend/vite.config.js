import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite dev server runs on port 5173 (the backend allows CORS from this origin).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
})
