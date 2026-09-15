"use client";

import { useEffect, useMemo, useState } from "react";

type Shipment = {
  id: number;
  trackingNumber: string;
  senderName: string;
  receiverName: string;
  origin: string;
  destination: string;
  status: string;
  createdAt?: string;
};

type User = {
  fullName?: string;
  name?: string;
  role?: string;
};

const API_URL = "http://localhost:8080";

export default function DashboardPage() {
  const [userName, setUserName] = useState("User");
  const [userRole, setUserRole] = useState("Customer");
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    const token = localStorage.getItem("token");

    if (storedUser) {
      try {
        const user: User = JSON.parse(storedUser);
        setUserName(user.fullName || user.name || "User");

        if (user.role) {
          setUserRole(
            user.role
              .replaceAll("_", " ")
              .toLowerCase()
              .replace(/\b\w/g, (char) => char.toUpperCase())
          );
        }
      } catch {
        setUserName("User");
      }
    }

    if (!token) {
      window.location.href = "/login";
      return;
    }

    fetch(`${API_URL}/api/shipments`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error("Failed to load shipments");
        }

        return response.json();
      })
      .then((data: Shipment[]) => {
        setShipments(Array.isArray(data) ? data : []);
      })
      .catch((error) => {
        console.error("Shipment loading error:", error);
        setShipments([]);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const stats = useMemo(() => {
    return {
      total: shipments.length,
      inTransit: shipments.filter(
        (shipment) => shipment.status === "IN_TRANSIT"
      ).length,
      outForDelivery: shipments.filter(
        (shipment) => shipment.status === "OUT_FOR_DELIVERY"
      ).length,
      delivered: shipments.filter(
        (shipment) => shipment.status === "DELIVERED"
      ).length,
    };
  }, [shipments]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  const getStatusStyle = (status: string) => {
    switch (status) {
      case "DELIVERED":
        return "bg-green-500/10 text-green-400 border-green-500/20";

      case "IN_TRANSIT":
        return "bg-blue-500/10 text-blue-400 border-blue-500/20";

      case "OUT_FOR_DELIVERY":
        return "bg-yellow-500/10 text-yellow-400 border-yellow-500/20";

      case "CANCELLED":
        return "bg-red-500/10 text-red-400 border-red-500/20";

      default:
        return "bg-cyan-500/10 text-cyan-400 border-cyan-500/20";
    }
  };

  const formatStatus = (status: string) => {
    return status
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  return (
    <main className="min-h-screen bg-slate-950 text-white">
      <div className="flex min-h-screen">

        {/* SIDEBAR */}
        <aside className="hidden w-64 shrink-0 border-r border-slate-800 bg-slate-900 lg:flex lg:flex-col">

          <div className="border-b border-slate-800 px-6 py-6">
            <h1 className="text-2xl font-extrabold">
              ShipTrack <span className="text-cyan-400">Pro</span>
            </h1>

            <p className="mt-1 text-xs text-slate-500">
              Delivery Visibility Platform
            </p>
          </div>

          <nav className="flex-1 space-y-2 px-4 py-6">

            <a
              href="/dashboard"
              className="flex items-center gap-3 rounded-xl bg-cyan-500/10 px-4 py-3 text-sm font-semibold text-cyan-400"
            >
              <span>🏠</span>
              Dashboard
            </a>

            <a
              href="/shipments"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>📦</span>
              Shipments
            </a>

            <a
              href="#tracking"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>📍</span>
              Live Tracking
            </a>

            <a
              href="#routes"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>🚚</span>
              Routes
            </a>

            <a
                href="/tracking-history"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>🕒</span>
              Tracking History
            </a>

            <a
              href="#analytics"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>📊</span>
              Analytics
            </a>

            <a
              href="#notifications"
              className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-white"
            >
              <span>🔔</span>
              Notifications
            </a>

          </nav>

          <div className="border-t border-slate-800 p-4">

            <div className="mb-3 rounded-xl bg-slate-800/50 p-3">
              <p className="truncate text-sm font-semibold">
                {userName}
              </p>

              <p className="mt-1 text-xs text-slate-500">
                {userRole}
              </p>
            </div>

            <button
              onClick={handleLogout}
              className="w-full rounded-xl border border-slate-700 px-4 py-2.5 text-sm font-semibold text-slate-300 transition hover:border-red-500/50 hover:bg-red-500/10 hover:text-red-400"
            >
              Logout
            </button>

          </div>
        </aside>

        {/* MAIN AREA */}
        <div className="flex min-w-0 flex-1 flex-col">

          {/* HEADER */}
          <header className="sticky top-0 z-10 border-b border-slate-800 bg-slate-950/95 backdrop-blur">

            <div className="flex items-center justify-between px-5 py-4 sm:px-8">

              <div>
                <p className="text-xs font-semibold uppercase tracking-widest text-cyan-400">
                  ShipTrack Pro
                </p>

                <h2 className="mt-1 text-lg font-bold sm:text-xl">
                  Shipment Dashboard
                </h2>
              </div>

              <div className="flex items-center gap-3">

                <button
                  className="hidden rounded-xl border border-slate-800 bg-slate-900 px-4 py-2 text-sm text-slate-300 transition hover:border-slate-700 hover:text-white sm:block"
                  onClick={() => {
                    window.location.href = "/shipments";
                  }}
                >
                  📦 My Shipments
                </button>

                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-cyan-500/10 font-bold text-cyan-400">
                  {userName.charAt(0).toUpperCase()}
                </div>

              </div>

            </div>

          </header>

          {/* CONTENT */}
          <section className="mx-auto w-full max-w-7xl flex-1 px-5 py-8 sm:px-8">

            {/* WELCOME */}
            <div className="mb-8">

              <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">

                <div>
                  <p className="text-sm font-semibold uppercase tracking-widest text-cyan-400">
                    Overview
                  </p>

                  <h1 className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">
                    Welcome back, {userName} 👋
                  </h1>

                  <p className="mt-2 max-w-2xl text-sm text-slate-400 sm:text-base">
                    Monitor your shipments, track deliveries and manage
                    logistics from one central dashboard.
                  </p>
                </div>

                <button
                  onClick={() => {
                    window.location.href = "/shipments";
                  }}
                  className="rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-5 py-3 text-sm font-bold shadow-lg shadow-cyan-500/10 transition hover:-translate-y-0.5 hover:from-cyan-400 hover:to-blue-500"
                >
                  + Create Shipment
                </button>

              </div>

            </div>

            {/* STAT CARDS */}
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

              {/* TOTAL */}
              <div className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:-translate-y-1 hover:border-cyan-500/40">

                <div className="flex items-start justify-between">

                  <div>
                    <p className="text-sm text-slate-400">
                      Total Shipments
                    </p>

                    <p className="mt-2 text-3xl font-extrabold">
                      {loading ? "..." : stats.total}
                    </p>
                  </div>

                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-500/10 text-xl">
                    📦
                  </div>

                </div>

                <p className="mt-4 text-xs text-slate-500">
                  All shipments created by you
                </p>

              </div>

              {/* IN TRANSIT */}
              <div className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:-translate-y-1 hover:border-blue-500/40">

                <div className="flex items-start justify-between">

                  <div>
                    <p className="text-sm text-slate-400">
                      In Transit
                    </p>

                    <p className="mt-2 text-3xl font-extrabold">
                      {loading ? "..." : stats.inTransit}
                    </p>
                  </div>

                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-xl">
                    🚚
                  </div>

                </div>

                <p className="mt-4 text-xs text-slate-500">
                  Shipments currently moving
                </p>

              </div>

              {/* OUT FOR DELIVERY */}
              <div className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:-translate-y-1 hover:border-yellow-500/40">

                <div className="flex items-start justify-between">

                  <div>
                    <p className="text-sm text-slate-400">
                      Out for Delivery
                    </p>

                    <p className="mt-2 text-3xl font-extrabold">
                      {loading ? "..." : stats.outForDelivery}
                    </p>
                  </div>

                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-yellow-500/10 text-xl">
                    🛵
                  </div>

                </div>

                <p className="mt-4 text-xs text-slate-500">
                  Delivery in progress
                </p>

              </div>

              {/* DELIVERED */}
              <div className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:-translate-y-1 hover:border-green-500/40">

                <div className="flex items-start justify-between">

                  <div>
                    <p className="text-sm text-slate-400">
                      Delivered
                    </p>

                    <p className="mt-2 text-3xl font-extrabold">
                      {loading ? "..." : stats.delivered}
                    </p>
                  </div>

                  <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-green-500/10 text-xl">
                    ✅
                  </div>

                </div>

                <p className="mt-4 text-xs text-slate-500">
                  Successfully delivered
                </p>

              </div>

            </div>

            {/* QUICK ACTIONS */}
            <div className="mt-8">

              <div className="mb-4">
                <h3 className="text-xl font-bold">
                  Quick Actions
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                  Frequently used shipment operations
                </p>
              </div>

              <div className="grid gap-4 md:grid-cols-3">

                <button
                  onClick={() => {
                    window.location.href = "/shipments";
                  }}
                  className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 text-left transition hover:-translate-y-1 hover:border-cyan-500/50"
                >
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-500/10 text-2xl">
                    ➕
                  </div>

                  <h4 className="mt-4 text-lg font-bold">
                    Create Shipment
                  </h4>

                  <p className="mt-2 text-sm leading-6 text-slate-400">
                    Create a new shipment and generate its tracking number.
                  </p>

                  <span className="mt-4 inline-block text-sm font-semibold text-cyan-400">
                    Create now →
                  </span>
                </button>

                <button
                  onClick={() => {
                    window.location.href = "/shipments";
                  }}
                  className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 text-left transition hover:-translate-y-1 hover:border-blue-500/50"
                >
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-2xl">
                    🔍
                  </div>

                  <h4 className="mt-4 text-lg font-bold">
                    Track Shipment
                  </h4>

                  <p className="mt-2 text-sm leading-6 text-slate-400">
                    View your shipments and monitor their current status.
                  </p>

                  <span className="mt-4 inline-block text-sm font-semibold text-blue-400">
                    Track now →
                  </span>
                </button>

                <button
                  onClick={() => {
                    document
                      .getElementById("analytics")
                      ?.scrollIntoView({ behavior: "smooth" });
                  }}
                  className="group rounded-2xl border border-slate-800 bg-slate-900 p-5 text-left transition hover:-translate-y-1 hover:border-purple-500/50"
                >
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-500/10 text-2xl">
                    📊
                  </div>

                  <h4 className="mt-4 text-lg font-bold">
                    Analytics
                  </h4>

                  <p className="mt-2 text-sm leading-6 text-slate-400">
                    View shipment performance and delivery statistics.
                  </p>

                  <span className="mt-4 inline-block text-sm font-semibold text-purple-400">
                    View analytics →
                  </span>
                </button>

              </div>

            </div>

            {/* RECENT SHIPMENTS */}
            <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900">

              <div className="flex flex-col gap-3 border-b border-slate-800 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">

                <div>
                  <h3 className="text-xl font-bold">
                    Recent Shipments
                  </h3>

                  <p className="mt-1 text-sm text-slate-500">
                    Latest shipment activity
                  </p>
                </div>

                <button
                  onClick={() => {
                    window.location.href = "/shipments";
                  }}
                  className="text-left text-sm font-semibold text-cyan-400 transition hover:text-cyan-300 sm:text-right"
                >
                  View All →
                </button>

              </div>

              {loading ? (
                <div className="px-6 py-14 text-center">
                  <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-400" />

                  <p className="mt-4 text-sm text-slate-400">
                    Loading shipments...
                  </p>
                </div>
              ) : shipments.length === 0 ? (
                <div className="px-6 py-14 text-center">

                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800 text-3xl">
                    📦
                  </div>

                  <h4 className="mt-5 text-lg font-semibold">
                    No shipments yet
                  </h4>

                  <p className="mt-2 text-sm text-slate-500">
                    Create your first shipment to start tracking deliveries.
                  </p>

                  <button
                    onClick={() => {
                      window.location.href = "/shipments";
                    }}
                    className="mt-6 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-3 text-sm font-bold transition hover:from-cyan-400 hover:to-blue-500"
                  >
                    Create Your First Shipment
                  </button>

                </div>
              ) : (
                <div className="overflow-x-auto">

                  <table className="w-full min-w-[750px] text-left">

                    <thead className="border-b border-slate-800 bg-slate-950/30">
                      <tr>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Tracking Number
                        </th>

                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Route
                        </th>

                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Receiver
                        </th>

                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Status
                        </th>
                      </tr>
                    </thead>

                    <tbody className="divide-y divide-slate-800">

                      {shipments.slice(0, 5).map((shipment) => (
                        <tr
                          key={shipment.id}
                          className="transition hover:bg-slate-800/30"
                        >

                          <td className="px-6 py-5">
                            <p className="font-semibold text-white">
                              {shipment.trackingNumber}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              Shipment #{shipment.id}
                            </p>
                          </td>

                          <td className="px-6 py-5">
                            <p className="text-sm text-slate-300">
                              {shipment.origin}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              ↓ {shipment.destination}
                            </p>
                          </td>

                          <td className="px-6 py-5 text-sm text-slate-300">
                            {shipment.receiverName}
                          </td>

                          <td className="px-6 py-5">
                            <span
                              className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getStatusStyle(
                                shipment.status
                              )}`}
                            >
                              {formatStatus(shipment.status)}
                            </span>
                          </td>

                        </tr>
                      ))}

                    </tbody>

                  </table>

                </div>
              )}

            </div>

            {/* PERFORMANCE SECTION */}
            <div
              id="analytics"
              className="mt-8 grid gap-5 lg:grid-cols-2"
            >

              {/* STATUS OVERVIEW */}
              <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                <h3 className="text-lg font-bold">
                  Shipment Status Overview
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                  Current distribution of your shipments
                </p>

                <div className="mt-6 space-y-5">

                  <StatusBar
                    label="Delivered"
                    value={stats.delivered}
                    total={stats.total}
                    icon="✅"
                  />

                  <StatusBar
                    label="In Transit"
                    value={stats.inTransit}
                    total={stats.total}
                    icon="🚚"
                  />

                  <StatusBar
                    label="Out for Delivery"
                    value={stats.outForDelivery}
                    total={stats.total}
                    icon="🛵"
                  />

                  <StatusBar
                    label="Created"
                    value={shipments.filter(
                      (shipment) => shipment.status === "CREATED"
                    ).length}
                    total={stats.total}
                    icon="📦"
                  />

                </div>

              </div>

              {/* PLATFORM INFO */}
              <div
                id="tracking"
                className="rounded-2xl border border-slate-800 bg-slate-900 p-6"
              >

                <h3 className="text-lg font-bold">
                  Delivery Visibility
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                  Your shipment monitoring center
                </p>

                <div className="mt-6 rounded-2xl border border-cyan-500/10 bg-cyan-500/5 p-5">

                  <div className="flex items-start gap-4">

                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-cyan-500/10 text-2xl">
                      📍
                    </div>

                    <div>
                      <h4 className="font-bold">
                        Real-time tracking
                      </h4>

                      <p className="mt-2 text-sm leading-6 text-slate-400">
                        Track shipment progress, delivery status and
                        logistics activity from a single platform.
                      </p>
                    </div>

                  </div>

                </div>

                <div className="mt-4 grid grid-cols-2 gap-4">

                  <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                    <p className="text-xs text-slate-500">
                      Active Shipments
                    </p>

                    <p className="mt-2 text-2xl font-bold">
                      {stats.inTransit + stats.outForDelivery}
                    </p>
                  </div>

                  <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
                    <p className="text-xs text-slate-500">
                      Delivery Rate
                    </p>

                    <p className="mt-2 text-2xl font-bold text-green-400">
                      {stats.total > 0
                        ? Math.round(
                            (stats.delivered / stats.total) * 100
                          )
                        : 0}
                      %
                    </p>
                  </div>

                </div>

              </div>

            </div>

          </section>

        </div>
      </div>
    </main>
  );
}

function StatusBar({
  label,
  value,
  total,
  icon,
}: {
  label: string;
  value: number;
  total: number;
  icon: string;
}) {
  const percentage =
    total > 0 ? Math.round((value / total) * 100) : 0;

  return (
    <div>

      <div className="mb-2 flex items-center justify-between">

        <div className="flex items-center gap-2">
          <span>{icon}</span>
          <span className="text-sm font-medium text-slate-300">
            {label}
          </span>
        </div>

        <span className="text-sm font-semibold text-slate-400">
          {value} ({percentage}%)
        </span>

      </div>

      <div className="h-2 overflow-hidden rounded-full bg-slate-800">

        <div
          className="h-full rounded-full bg-cyan-400 transition-all duration-700"
          style={{
            width: `${percentage}%`,
          }}
        />

      </div>

    </div>
  );
}