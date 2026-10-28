import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';

// En dev, Vite relaie /api et /ws vers l'API, comme nginx en production.
// Par défaut : l'API lancée dans Docker (`make api`, port 8081).
// API lancée en natif (`./mvnw spring-boot:run`) : API_PROXY_TARGET=http://localhost:8080
const apiTarget = process.env.API_PROXY_TARGET ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    proxy: {
      '/api': apiTarget,
      '/ws': { target: apiTarget, ws: true },
    },
  },
});
