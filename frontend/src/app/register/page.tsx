"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { Alert } from "@/components/Alert";
import { AuthShell } from "@/components/AuthShell";
import { apiRequest } from "@/lib/api";
import { setFlash } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") ?? "");
    const confirmation = String(form.get("confirmPassword") ?? "");

    if (password !== confirmation) {
      setMessage("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName: String(form.get("fullName") ?? "").trim(),
          email: String(form.get("email") ?? "").trim(),
          phone: String(form.get("phone") ?? "").trim() || null,
          role: String(form.get("role") ?? ""),
          password,
        }),
      });
      setFlash("Account created successfully. Sign in to continue.");
      router.push("/login");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to create the account.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      register
      eyebrow="One shared source of truth"
      title="Move freight with confidence."
      description="Create a secure workspace for shipment updates, hand-offs, and end-to-end delivery visibility."
    >
      <div className="heading-block">
        <span className="eyebrow dark">Get started</span>
        <h2>Create your account</h2>
        <p>Set up your profile to access the delivery platform.</p>
      </div>
      <Alert message={message} />
      <form onSubmit={handleSubmit}>
        <div className="field-grid">
          <div className="field full-span">
            <label htmlFor="fullName">Full name</label>
            <input id="fullName" name="fullName" autoComplete="name" maxLength={120} placeholder="Your full name" required />
          </div>
          <div className="field">
            <label htmlFor="email">Work email</label>
            <input id="email" name="email" type="email" autoComplete="email" placeholder="name@company.com" required />
          </div>
          <div className="field">
            <label htmlFor="phone">Phone <span className="optional">Optional</span></label>
            <input id="phone" name="phone" type="tel" autoComplete="tel" maxLength={25} placeholder="+91 98765 43210" />
          </div>
          <div className="field full-span">
            <label htmlFor="role">Your role</label>
            <select id="role" name="role" defaultValue="" required>
              <option value="" disabled>Select your role</option>
              <option value="BUSINESS_CLIENT">Business client</option>
              <option value="LOGISTICS_OPERATOR">Logistics operator</option>
              <option value="CUSTOMER">Customer</option>
              <option value="SUPPORT_AGENT">Support agent</option>
            </select>
            <small className="field-help">Shipment management is available to business clients and logistics operators.</small>
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="new-password" minLength={8} placeholder="At least 8 characters" required />
              <button className="password-toggle" type="button" onClick={() => setShowPassword((visible) => !visible)} aria-pressed={showPassword}>
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </div>
          <div className="field">
            <label htmlFor="confirmPassword">Confirm password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" minLength={8} placeholder="Repeat your password" required />
          </div>
        </div>
        <label className="check-row">
          <input name="terms" type="checkbox" required />
          <span>I confirm that these details are accurate and may be used for my ShipTrack account.</span>
        </label>
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" />Creating account...</> : "Create account"}
        </button>
      </form>
      <p className="switch-copy">Already have an account? <Link href="/login">Sign in</Link></p>
    </AuthShell>
  );
}
