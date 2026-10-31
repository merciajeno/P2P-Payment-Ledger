import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/login": env.VITE_API_PROXY_TARGET || "http://localhost:8080",
        "/register": env.VITE_API_PROXY_TARGET || "http://localhost:8080",
        "/wallet": env.VITE_API_PROXY_TARGET || "http://localhost:8080",
        "/transactions": env.VITE_API_PROXY_TARGET || "http://localhost:8080",
        "/api/ledger": env.VITE_API_PROXY_TARGET || "http://localhost:8080",
      },
    },
  };
});
