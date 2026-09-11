"use client";

import { useEffect, useState } from "react";

export default function DashboardPage() {
  const [userName, setUserName] = useState("User");

  useEffect(() => {
    const storedUser = localStorage.getItem("user");

    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        setUserName(user.fullName || user.name || "User");
      } catch {
        setUserName("User");
      }
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  return (
    <main className="min-h-screen bg-slate-950 text-white">
      {/* HEADER */}
      <header className="border-b border-slate-800 bg-slate-900">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5">
          <div>
            <h1 className="text-2xl font-extrabold">
              ShipTrack <span className="text-cyan-400">Pro</span>
            </h1>
            <p className="text-sm text-slate-400">
              Shipment Tracking & Delivery Visibility
            </p>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold">{userName}</p>
              <p className="text-xs text-slate-400">Customer</p>
            </div>

            <button
              onClick={handleLogout}
              className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-semibold transition hover:border-red-400 hover:bg-red-500/10 hover:text-red-400"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* CONTENT */}
      <section className="mx-auto max-w-7xl px-6 py-10">

        {/* WELCOME */}
        <div className="mb-8">
          <p className="text-sm font-semibold uppercase tracking-widest text-cyan-400">
            Dashboard
          </p>

          <h2 className="mt-2 text-3xl font-bold sm:text-4xl">
            Welcome back, {userName} 👋
          </h2>

          <p className="mt-2 text-slate-400">
            Manage and track all your shipments from one place.
          </p>
        </div>

        {/* STATS */}
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-500/10 text-2xl">
              📦
            </div>
            <p className="text-sm text-slate-400">Total Shipments</p>
            <p className="mt-2 text-3xl font-bold">0</p>
          </div>

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-2xl">
              🚚
            </div>
            <p className="text-sm text-slate-400">In Transit</p>
            <p className="mt-2 text-3xl font-bold">0</p>
          </div>

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-yellow-500/10 text-2xl">
              ⏱️
            </div>
            <p className="text-sm text-slate-400">Out for Delivery</p>
            <p className="mt-2 text-3xl font-bold">0</p>
          </div>

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-green-500/10 text-2xl">
              ✅
            </div>
            <p className="text-sm text-slate-400">Delivered</p>
            <p className="mt-2 text-3xl font-bold">0</p>
          </div>

        </div>

        {/* QUICK ACTIONS */}
        <div className="mt-10">
          <h3 className="mb-5 text-xl font-bold">Quick Actions</h3>

          <div className="grid gap-5 md:grid-cols-3">

            <button className="group rounded-2xl border border-slate-800 bg-slate-900 p-6 text-left transition hover:-translate-y-1 hover:border-cyan-500">
              <div className="mb-4 text-3xl">➕</div>

              <h4 className="text-lg font-bold">
                Create Shipment
              </h4>

              <p className="mt-2 text-sm text-slate-400">
                Create a new shipment and generate a tracking number.
              </p>

              <span className="mt-5 inline-block text-sm font-semibold text-cyan-400">
                Create now →
              </span>
            </button>

            <button className="group rounded-2xl border border-slate-800 bg-slate-900 p-6 text-left transition hover:-translate-y-1 hover:border-blue-500">
              <div className="mb-4 text-3xl">🔍</div>

              <h4 className="text-lg font-bold">
                Track Shipment
              </h4>

              <p className="mt-2 text-sm text-slate-400">
                Search a shipment using its tracking number.
              </p>

              <span className="mt-5 inline-block text-sm font-semibold text-blue-400">
                Track now →
              </span>
            </button>

            <button className="group rounded-2xl border border-slate-800 bg-slate-900 p-6 text-left transition hover:-translate-y-1 hover:border-purple-500">
              <div className="mb-4 text-3xl">📊</div>

              <h4 className="text-lg font-bold">
                Analytics
              </h4>

              <p className="mt-2 text-sm text-slate-400">
                View shipment performance and delivery statistics.
              </p>

              <span className="mt-5 inline-block text-sm font-semibold text-purple-400">
                View analytics →
              </span>
            </button>

          </div>
        </div>

        {/* RECENT SHIPMENTS */}
        <div className="mt-10 rounded-2xl border border-slate-800 bg-slate-900">

          <div className="flex items-center justify-between border-b border-slate-800 px-6 py-5">
            <div>
              <h3 className="text-xl font-bold">
                Recent Shipments
              </h3>
              <p className="mt-1 text-sm text-slate-400">
                Your latest shipment activity
              </p>
            </div>

            <button className="text-sm font-semibold text-cyan-400 hover:text-cyan-300">
              View All
            </button>
          </div>

          <div className="px-6 py-14 text-center">
            <div className="text-5xl">📦</div>

            <h4 className="mt-4 text-lg font-semibold">
              No shipments yet
            </h4>

            <p className="mt-2 text-sm text-slate-400">
              Create your first shipment to see it here.
            </p>

            <button className="mt-6 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-3 font-bold transition hover:from-cyan-400 hover:to-blue-500">
              Create Your First Shipment
            </button>
          </div>

        </div>

      </section>
    </main>
  );
}