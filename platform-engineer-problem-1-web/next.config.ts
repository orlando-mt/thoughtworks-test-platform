import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    // Genera .next/standalone: un servidor Node mínimo para la imagen
    output: "standalone",
};

export default nextConfig;