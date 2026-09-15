"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

type LoginRole = "CUSTOMER" | "ADMIN";

export default function LoginPage() {
  const router = useRouter();

  const [loginRole, setLoginRole] = useState<LoginRole>("CUSTOMER");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email,
            password,
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message || "Invalid email or password"
        );
      }

      const role = data.user?.role;

      // Prevent wrong portal access
      if (
        loginRole === "ADMIN" &&
        role !== "ADMIN" &&
        role !== "ADMINISTRATOR"
      ) {
        throw new Error(
          "This account does not have Administrator access."
        );
      }

      if (
        loginRole === "CUSTOMER" &&
        role !== "CUSTOMER"
      ) {
        throw new Error(
          "This account is not a Customer account."
        );
      }

      // Save login information
      localStorage.setItem("token", data.token);
      localStorage.setItem(
        "user",
        JSON.stringify(data.user)
      );

      setMessage("Login successful!");

      setTimeout(() => {
        if (
          role === "ADMIN" ||
          role === "ADMINISTRATOR"
        ) {
          router.push("/admin");
        } else {
          router.push("/dashboard");
        }
      }, 500);
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Unable to connect to server."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#07111f] px-4 py-10 text-white">
      <div className="mx-auto flex min-h-[90vh] max-w-5xl items-center justify-center">

        <div className="grid w-full overflow-hidden rounded-3xl border border-slate-700 bg-[#0d1b2a] shadow-2xl md:grid-cols-2">

          {/* LEFT */}
          <div className="relative hidden overflow-hidden bg-gradient-to-br from-[#0b3b66] via-[#075985] to-[#0e7490] p-12 md:flex md:flex-col md:justify-center">

            <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-cyan-400/20 blur-3xl" />

            <div className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-blue-500/20 blur-3xl" />

            <div className="relative z-10">

              <div className="mb-8 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/10 text-4xl backdrop-blur">
                📦
              </div>

              <h1 className="text-4xl font-extrabold">
                ShipTrack{" "}
                <span className="text-cyan-300">
                  Pro
                </span>
              </h1>

              <p className="mt-5 max-w-md text-lg leading-8 text-blue-100">
                Your complete shipment tracking and
                delivery visibility platform.
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

          {/* RIGHT */}
          <div className="bg-[#0d1b2a] p-7 sm:p-10 md:p-12">

            <div className="mb-8">

              <p className="text-sm font-bold uppercase tracking-[0.2em] text-cyan-400">
                ShipTrack Pro Portal
              </p>

              <h2 className="mt-3 text-3xl font-bold">
                Login
              </h2>

              <p className="mt-2 text-sm text-slate-400">
                Select your portal and sign in securely.
              </p>

            </div>

            {/* ROLE SELECTOR */}
            <div className="mb-7">

              <label className="mb-3 block text-sm font-semibold text-slate-200">
                Login As
              </label>

              <div className="grid grid-cols-2 gap-3">

                <button
                  type="button"
                  onClick={() => {
                    setLoginRole("CUSTOMER");
                    setMessage("");
                  }}
                  className={`rounded-xl border px-4 py-4 text-left transition ${
                    loginRole === "CUSTOMER"
                      ? "border-cyan-400 bg-cyan-500/10 text-cyan-300"
                      : "border-slate-700 bg-[#07111f] text-slate-400 hover:border-slate-500"
                  }`}
                >
                  <div className="text-2xl">
                    👤
                  </div>

                  <div className="mt-2 font-bold">
                    Customer
                  </div>

                  <div className="mt-1 text-xs opacity-70">
                    Track shipments
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setLoginRole("ADMIN");
                    setMessage("");
                  }}
                  className={`rounded-xl border px-4 py-4 text-left transition ${
                    loginRole === "ADMIN"
                      ? "border-cyan-400 bg-cyan-500/10 text-cyan-300"
                      : "border-slate-700 bg-[#07111f] text-slate-400 hover:border-slate-500"
                  }`}
                >
                  <div className="text-2xl">
                    🛡️
                  </div>

                  <div className="mt-2 font-bold">
                    Administrator
                  </div>

                  <div className="mt-1 text-xs opacity-70">
                    Manage platform
                  </div>
                </button>

              </div>

            </div>

            {/* SELECTED PORTAL */}
            <div className="mb-6 rounded-xl border border-cyan-500/20 bg-cyan-500/5 px-4 py-3">

              <p className="text-xs uppercase tracking-wider text-slate-500">
                Selected Portal
              </p>

              <p className="mt-1 font-bold text-cyan-400">
                {loginRole === "ADMIN"
                  ? "Administrator Portal"
                  : "Customer Portal"}
              </p>

            </div>

            {/* LOGIN FORM */}
            <form
              onSubmit={handleLogin}
              className="space-y-5"
            >

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
                  placeholder={
                    loginRole === "ADMIN"
                      ? "Administrator email"
                      : "Customer email"
                  }
                  required
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
                  placeholder="Enter your password"
                  required
                  className="w-full rounded-xl border border-slate-600 bg-[#07111f] px-4 py-3.5 text-white placeholder:text-slate-500 outline-none transition focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />

              </div>

              {/* BUTTON */}
              <button
                type="submit"
                disabled={loading}
                className="mt-3 w-full rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-5 py-3.5 font-bold text-white shadow-lg transition hover:from-cyan-400 hover:to-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading
                  ? "Logging in..."
                  : `Login as ${
                      loginRole === "ADMIN"
                        ? "Administrator"
                        : "Customer"
                    }`}
              </button>

            </form>

            {/* MESSAGE */}
            {message && (
              <div
                className={`mt-5 rounded-xl border px-4 py-3 text-center text-sm font-medium ${
                  message === "Login successful!"
                    ? "border-green-500/30 bg-green-500/10 text-green-300"
                    : "border-red-500/30 bg-red-500/10 text-red-300"
                }`}
              >
                {message}
              </div>
            )}

            {/* REGISTER */}
            {loginRole === "CUSTOMER" && (
              <p className="mt-7 text-center text-sm text-slate-400">
                Don't have a Customer account?{" "}
                <a
                  href="/register"
                  className="font-bold text-cyan-400 hover:text-cyan-300"
                >
                  Create Account
                </a>
              </p>
            )}

            {loginRole === "ADMIN" && (
              <p className="mt-7 text-center text-xs text-slate-500">
                Administrator accounts are managed
                securely by the system.
              </p>
            )}

          </div>
        </div>
      </div>
    </main>
  );
}