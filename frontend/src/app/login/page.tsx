"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { Alert } from "@/components/Alert";
import { AuthShell } from "@/components/AuthShell";
import { apiRequest } from "@/lib/api";
import { getAuth, saveAuth, takeFlash } from "@/lib/auth";
import type { AuthSession } from "@/lib/types";

export default function LoginPage() {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [flash, setFlash] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (getAuth()) {
        router.replace("/dashboard");
        return;
      }
      setFlash(takeFlash() ?? "");
    }, 0);

    return () => window.clearTimeout(timer);
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setFlash("");
    const form = new FormData(event.currentTarget);

    setLoading(true);
    try {
      const session = await apiRequest<AuthSession>("/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: String(form.get("email") ?? "").trim(),
          password: String(form.get("password") ?? ""),
        }),
      });
      saveAuth(session, form.get("remember") === "on");
      router.push("/dashboard");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to sign in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      eyebrow="Delivery visibility, simplified"
      title={<>Every shipment.<br />Clearly in sight.</>}
      description="Coordinate teams, follow every milestone, and keep deliveries moving from one secure workspace."
    >
      <div className="heading-block">
        <span className="eyebrow dark">Welcome back</span>
        <h2>Sign in to your account</h2>
        <p>Enter your registered email and password to continue.</p>
      </div>
      <Alert message={flash} success />
      <Alert message={message} />
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Email address</label>
          <input id="email" name="email" type="email" autoComplete="email" placeholder="name@company.com" required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <div className="password-field">
            <input id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="current-password" placeholder="Enter your password" required />
            <button className="password-toggle" type="button" onClick={() => setShowPassword((visible) => !visible)} aria-pressed={showPassword}>
              {showPassword ? "Hide" : "Show"}
            </button>
          </div>
        </div>
        <label className="check-row">
          <input name="remember" type="checkbox" />
          <span>Keep me signed in on this device</span>
        </label>
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" />Signing in...</> : "Sign in"}
        </button>
      </form>
      <p className="switch-copy">New to ShipTrack? <Link href="/register">Create an account</Link></p>
    </AuthShell>
  );
}
