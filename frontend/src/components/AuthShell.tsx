import type { ReactNode } from "react";
import { Brand } from "./Brand";

interface AuthShellProps {
  eyebrow: string;
  title: ReactNode;
  description: string;
  children: ReactNode;
  register?: boolean;
}

export function AuthShell({ eyebrow, title, description, children, register = false }: AuthShellProps) {
  return (
    <main className={`auth-layout ${register ? "auth-layout-register" : ""}`}>
      <header className="auth-header">
        <Brand light href="/login" />
      </header>

      <div className="auth-content">
        <section className="brand-panel" aria-label="ShipTrack introduction">
        <div className="brand-copy">
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        </section>

        <section className="form-panel">
          <div className={`form-wrap ${register ? "form-wrap-wide" : ""}`}>
            {children}
          </div>
        </section>
      </div>

      <footer className="auth-footer">
        <span>ShipTrack</span>
        <span>Shipment Tracking &amp; Delivery Visibility Platform</span>
      </footer>
    </main>
  );
}
