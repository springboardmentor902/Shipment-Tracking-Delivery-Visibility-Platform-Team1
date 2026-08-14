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
      <div className="auth-orb auth-orb-one" aria-hidden="true" />
      <div className="auth-orb auth-orb-two" aria-hidden="true" />

      <header className="auth-header">
        <Brand light href="/login" />
        <span className="system-status"><i /> Operations platform online</span>
      </header>

      <div className="auth-content">
        <section className="brand-panel" aria-label="ShipTrack introduction">
        <div className="brand-copy">
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>

          <div className="capability-row" aria-label="Platform capabilities">
            <span>Role-based access</span>
            <span>Live milestones</span>
            <span>Secure operations</span>
          </div>

          <div className="shipment-preview" aria-label="Example shipment status">
            <div className="preview-head">
              <span><i /> Shipment STP-78342</span>
              <strong>In transit</strong>
            </div>
            <div className="preview-route">
              <div><small>From</small><strong>DEL</strong><span>New Delhi</span></div>
              <div className="route-progress"><span /></div>
              <div><small>To</small><strong>BOM</strong><span>Mumbai</span></div>
            </div>
            <div className="preview-foot">
              <span>Updated 2 minutes ago</span>
              <span>{register ? "Secure onboarding" : "End-to-end visibility"}</span>
            </div>
          </div>
        </div>
        </section>

        <section className="form-panel">
          <div className={`form-wrap ${register ? "form-wrap-wide" : ""}`}>
            {children}
          </div>
        </section>
      </div>

      <footer className="auth-footer">
        <span>© 2026 ShipTrack Technologies</span>
        <span>Visibility that moves business forward.</span>
      </footer>
    </main>
  );
}
