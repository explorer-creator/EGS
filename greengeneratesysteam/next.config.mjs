/** @type {import('next').NextConfig} */
const nextConfig = {
  /**
   * 允许主站（Vite 5173 等）通过 iframe 嵌入本演示页，避免部分环境下子页拒绝被框住。
   */
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          {
            key: 'Content-Security-Policy',
            value:
              "frame-ancestors 'self' http://localhost:5173 http://127.0.0.1:5173 http://localhost:5174 http://127.0.0.1:5174 http://localhost:5175 http://127.0.0.1:5175 http://localhost:5176 http://127.0.0.1:5176 http://localhost:5177 http://127.0.0.1:5177"
          }
        ]
      }
    ]
  }
};

export default nextConfig;
