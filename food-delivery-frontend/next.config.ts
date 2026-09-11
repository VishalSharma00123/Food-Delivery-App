import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,
  // Smaller Docker image; used by food-delivery-frontend/Dockerfile
  output: "standalone",
};

export default nextConfig;
