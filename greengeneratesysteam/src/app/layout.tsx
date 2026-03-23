import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AI 绿能排产 & 碳排推演模拟器",
  description: "AI Green Production Scheduling & Carbon Emission Simulator - MVP",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="antialiased">{children}</body>
    </html>
  );
}
