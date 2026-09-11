"use client";

import { FormEvent, useState } from "react";

export default function RegisterPage() {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    setMessage("");
    setSuccess(false);
    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/register",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            fullName: fullName.trim(),
            email: email.trim(),
            phone: phone.trim(),
            password,
            role: "CUSTOMER",
          }),
        }
      );

      const text = await response.text();

      let data: any = {};

      try {
        data = text ? JSON.parse(text) : {};
      } catch {
        data = {};
      }

      if (!response.ok) {
        throw new Error(
          data.message ||
            data.error ||
            `Registration failed (${response.status})`
        );
      }

      setSuccess(true);
      setMessage(
        "Account created successfully! You can now login."
      );

      setFullName("");
      setEmail("");
      setPhone("");
      setPassword("");
    } catch (error) {
      console.error("Registration error:", error);

      setSuccess(false);

      if (error instanceof TypeError) {
        setMessage(
          "Unable to connect to server. Please make sure Spring Boot is running on port 8080."
        );
      } else {
        setMessage(
          error instanceof Error
            ? error.message
            : "Something went wrong. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#07111f] px-4 py-10 text-white">
      <div className="mx-auto flex min-h-[90vh] max-w-6xl items-center justify-center">
        <div className="grid w-full overflow-hidden rounded-3xl border border-slate-700 bg-[#0d1b2a] shadow-2xl md:grid-cols-2">

          {/* LEFT SIDE */}
          <div className="relative hidden overflow-hidden bg-gradient-to-br from-[#0b3b66] via-[#075985] to-[#0e7490] p-12 md:flex md:flex-col md:justify-center">

            <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-cyan-400/20 blur-3xl" />

            <div className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-blue-500/20 blur-3xl" />

            <div className="relative z-10">

              <div className="mb-8 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/10 text-4xl backdrop-blur">
                📦
              </div>

              <h1 className="text-4xl font-extrabold tracking-tight">
                ShipTrack{" "}
                <span className="text-cyan-300">
                  Pro
                </span>
              </h1>

              <p className="mt-5 max-w-md text-lg leading-8 text-blue-100">
                Your complete shipment tracking and delivery
                visibility platform.
              </p>

              <div className="mt-10 space-y-5">

                <div className="flex items-center gap-4">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">
                    ✓
                  </span>
                  <span>
                    Real-time shipment tracking
                  </span>
                </div>

                <div className="flex items-center gap-4">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">
                    ✓
                  </span>
                  <span>
                    Complete delivery visibility
                  </span>
                </div>

                <div className="flex items-center gap-4">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">
                    ✓
                  </span>
                  <span>
                    Secure authentication
                  </span>
                </div>

                <div className="flex items-center gap-4">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">
                    ✓
                  </span>
                  <span>
                    Smart logistics management
                  </span>
                </div>

              </div>
            </div>
          </div>

          {/* RIGHT SIDE */}
          <div className="bg-[#0d1b2a] p-7 sm:p-10 md:p-12">

            <div className="mb-8">
              <p className="text-sm font-bold uppercase tracking-[0.2em] text-cyan-400">
                Get Started
              </p>

              <h2 className="mt-3 text-3xl font-bold text-white">
                Create Account
              </h2>

              <p className="mt-2 text-sm text-slate-400">
                Join ShipTrack Pro and manage your shipments easily.
              </p>
            </div>

            <form
              onSubmit={handleRegister}
              className="space-y-5"
            >

              {/* FULL NAME */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Full Name
                </label>

                <input
                  type="text"
                  value={fullName}
                  onChange={(e) =>
                    setFullName(e.target.value)
                  }
                  placeholder="Enter your full name"
                  required
                  className="w-full rounded-xl border border-slate-600 bg-[#07111f] px-4 py-3.5 text-white placeholder:text-slate-500 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              {/* EMAIL */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Email Address
                </label>

                <input
                  type="email"
                  value={email}
                  onChange={(e) =>
                    setEmail(e.target.value)
                  }
                  placeholder="Enter your email"
                  required
                  className="w-full rounded-xl border border-slate-600 bg-[#07111f] px-4 py-3.5 text-white placeholder:text-slate-500 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              {/* PHONE */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Phone Number
                </label>

                <input
                  type="tel"
                  value={phone}
                  onChange={(e) =>
                    setPhone(e.target.value)
                  }
                  placeholder="Enter your phone number"
                  className="w-full rounded-xl border border-slate-600 bg-[#07111f] px-4 py-3.5 text-white placeholder:text-slate-500 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              {/* PASSWORD */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Password
                </label>

                <input
                  type="password"
                  value={password}
                  onChange={(e) =>
                    setPassword(e.target.value)
                  }
                  placeholder="Create a password"
                  required
                  minLength={6}
                  className="w-full rounded-xl border border-slate-600 bg-[#07111f] px-4 py-3.5 text-white placeholder:text-slate-500 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              {/* BUTTON */}
              <button
                type="submit"
                disabled={loading}
                className="mt-3 w-full rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-5 py-3.5 font-bold text-white shadow-lg shadow-cyan-500/20 transition hover:from-cyan-400 hover:to-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading
                  ? "Creating Account..."
                  : "Create Account"}
              </button>
            </form>

            {/* MESSAGE */}
            {message && (
              <div
                className={`mt-5 rounded-xl border px-4 py-3 text-center text-sm font-medium ${
                  success
                    ? "border-green-500/30 bg-green-500/10 text-green-400"
                    : "border-red-500/30 bg-red-500/10 text-red-400"
                }`}
              >
                {message}
              </div>
            )}

            <p className="mt-7 text-center text-sm text-slate-400">
              Already have an account?{" "}
              <a
                href="/login"
                className="font-bold text-cyan-400 transition hover:text-cyan-300"
              >
                Login
              </a>
            </p>

          </div>
        </div>
      </div>
    </main>
  );
}