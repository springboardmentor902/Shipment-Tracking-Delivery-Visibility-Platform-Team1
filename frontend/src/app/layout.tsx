import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "ShipTrack Pro",
    template: "%s | ShipTrack Pro",
  },
  description: "Shipment tracking and delivery visibility platform",
};

export const viewport: Viewport = {
  colorScheme: "dark",
  themeColor: "#070a12",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" data-scroll-behavior="smooth">
      <body>{children}</body>
    </html>
  );
}
