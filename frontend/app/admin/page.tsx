"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type User = {
  fullName?: string;
  email?: string;
  role?: string;
};

type Shipment = {
  id: number;
  trackingNumber?: string;
  senderName?: string;
  receiverName?: string;
  status?: string;
  origin?: string;
  destination?: string;
};

export default function AdminPage() {
  const router = useRouter();

  const [user, setUser] = useState<User | null>(null);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedUser = localStorage.getItem("user");
    const token = localStorage.getItem("token");

    if (!savedUser || !token) {
      router.push("/login");
      return;
    }

    try {
      const parsedUser = JSON.parse(savedUser);
      setUser(parsedUser);

      if (
        parsedUser.role !== "ADMIN" &&
        parsedUser.role !== "ADMINISTRATOR"
      ) {
        router.push("/dashboard");
        return;
      }

      fetchShipments(token);
    } catch {
      localStorage.removeItem("user");
      localStorage.removeItem("token");
      router.push("/login");
    }
  }, [router]);

  const fetchShipments = async (token: string) => {
    try {
      const response = await fetch(
        "http://localhost:8080/api/shipments",
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.ok) {
        const data = await response.json();
        setShipments(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error("Failed to load shipments", error);
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    router.push("/login");
  };

  const total = shipments.length;

  const inTransit = shipments.filter(
    (s) => s.status === "IN_TRANSIT"
  ).length;

  const delivered = shipments.filter(
    (s) => s.status === "DELIVERED"
  ).length;

  const pending = shipments.filter(
    (s) =>
      s.status !== "DELIVERED" &&
      s.status !== "IN_TRANSIT"
  ).length;

  return (
    <main className="min-h-screen bg-[#07111f] text-white">
      <div className="flex min-h-screen">

        {/* SIDEBAR */}
        <aside className="hidden w-64 border-r border-slate-800 bg-[#0b1625] p-6 md:block">
          <div className="mb-10">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-500/20 text-2xl">
                📦
              </div>

              <div>
                <h1 className="text-xl font-extrabold">
                  ShipTrack <span className="text-cyan-400">Pro</span>
                </h1>
                <p className="text-xs text-slate-500">
                  Administration
                </p>
              </div>
            </div>
          </div>

          <nav className="space-y-2">
            <button className="w-full rounded-xl bg-cyan-500/10 px-4 py-3 text-left font-semibold text-cyan-400">
              📊 Dashboard
            </button>

            <button
              onClick={() => router.push("/shipments")}
              className="w-full rounded-xl px-4 py-3 text-left text-slate-300 transition hover:bg-slate-800"
            >
              📦 Shipments
            </button>

            <button
              onClick={() => router.push("/routes")}
              className="w-full rounded-xl px-4 py-3 text-left text-slate-300 transition hover:bg-slate-800"
            >
              🛣️ Routes
            </button>

            <button
              onClick={() => router.push("/tracking")}
              className="w-full rounded-xl px-4 py-3 text-left text-slate-300 transition hover:bg-slate-800"
            >
              📍 Live Tracking
            </button>

            <button
              onClick={() => router.push("/tracking-history")}
              className="w-full rounded-xl px-4 py-3 text-left text-slate-300 transition hover:bg-slate-800"
            >
              🕒 Tracking History
            </button>
          </nav>

          <div className="mt-auto pt-10">
            <button
              onClick={logout}
              className="w-full rounded-xl border border-red-500/30 px-4 py-3 text-left font-semibold text-red-400 transition hover:bg-red-500/10"
            >
              🚪 Logout
            </button>
          </div>
        </aside>

        {/* MAIN */}
        <section className="flex-1 p-6 md:p-10">

          {/* HEADER */}
          <div className="mb-10 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
            <div>
              <p className="text-sm font-bold uppercase tracking-[0.2em] text-cyan-400">
                Admin Control Center
              </p>

              <h2 className="mt-2 text-3xl font-extrabold">
                Welcome, {user?.fullName || "Administrator"}
              </h2>

              <p className="mt-2 text-slate-400">
                Manage shipments, routes and delivery visibility.
              </p>
            </div>

            <div className="rounded-xl border border-slate-700 bg-[#0d1b2a] px-5 py-3">
              <p className="text-xs text-slate-500">
                Current Role
              </p>

              <p className="font-bold text-cyan-400">
                Administrator
              </p>
            </div>
          </div>

          {/* STATS */}
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">

            <div className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6">
              <p className="text-sm text-slate-400">
                Total Shipments
              </p>

              <p className="mt-3 text-3xl font-extrabold">
                {total}
              </p>

              <p className="mt-2 text-xs text-cyan-400">
                Platform wide
              </p>
            </div>

            <div className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6">
              <p className="text-sm text-slate-400">
                In Transit
              </p>

              <p className="mt-3 text-3xl font-extrabold">
                {inTransit}
              </p>

              <p className="mt-2 text-xs text-blue-400">
                Currently moving
              </p>
            </div>

            <div className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6">
              <p className="text-sm text-slate-400">
                Delivered
              </p>

              <p className="mt-3 text-3xl font-extrabold">
                {delivered}
              </p>

              <p className="mt-2 text-xs text-green-400">
                Successfully completed
              </p>
            </div>

            <div className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6">
              <p className="text-sm text-slate-400">
                Pending
              </p>

              <p className="mt-3 text-3xl font-extrabold">
                {pending}
              </p>

              <p className="mt-2 text-xs text-yellow-400">
                Requires monitoring
              </p>
            </div>

          </div>

          {/* QUICK ACTIONS */}
          <div className="mt-8">
            <h3 className="mb-4 text-xl font-bold">
              Quick Actions
            </h3>

            <div className="grid gap-4 md:grid-cols-3">

              <button
                onClick={() => router.push("/shipments")}
                className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6 text-left transition hover:-translate-y-1 hover:border-cyan-500/50"
              >
                <div className="text-3xl">📦</div>

                <h4 className="mt-4 font-bold">
                  Manage Shipments
                </h4>

                <p className="mt-2 text-sm text-slate-400">
                  View and manage platform shipments.
                </p>
              </button>

              <button
                onClick={() => router.push("/routes")}
                className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6 text-left transition hover:-translate-y-1 hover:border-cyan-500/50"
              >
                <div className="text-3xl">🛣️</div>

                <h4 className="mt-4 font-bold">
                  Route Management
                </h4>

                <p className="mt-2 text-sm text-slate-400">
                  Monitor shipment routes and drivers.
                </p>
              </button>

              <button
                onClick={() => router.push("/tracking")}
                className="rounded-2xl border border-slate-800 bg-[#0d1b2a] p-6 text-left transition hover:-translate-y-1 hover:border-cyan-500/50"
              >
                <div className="text-3xl">📍</div>

                <h4 className="mt-4 font-bold">
                  Live Tracking
                </h4>

                <p className="mt-2 text-sm text-slate-400">
                  Monitor shipment movement in real time.
                </p>
              </button>

            </div>
          </div>

          {/* RECENT SHIPMENTS */}
          <div className="mt-8 rounded-2xl border border-slate-800 bg-[#0d1b2a]">

            <div className="border-b border-slate-800 p-6">
              <h3 className="text-xl font-bold">
                Recent Shipments
              </h3>

              <p className="mt-1 text-sm text-slate-400">
                Latest shipment activity across the platform.
              </p>
            </div>

            {loading ? (
              <div className="p-8 text-center text-slate-400">
                Loading shipments...
              </div>
            ) : shipments.length === 0 ? (
              <div className="p-8 text-center text-slate-400">
                No shipments available.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-slate-800 text-sm text-slate-500">
                      <th className="px-6 py-4">Tracking</th>
                      <th className="px-6 py-4">Origin</th>
                      <th className="px-6 py-4">Destination</th>
                      <th className="px-6 py-4">Status</th>
                    </tr>
                  </thead>

                  <tbody>
                    {shipments.slice(0, 8).map((shipment) => (
                      <tr
                        key={shipment.id}
                        className="border-b border-slate-800 last:border-0"
                      >
                        <td className="px-6 py-4 font-semibold text-cyan-400">
                          {shipment.trackingNumber || `SHIP-${shipment.id}`}
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-300">
                          {shipment.origin || shipment.senderName || "—"}
                        </td>

                        <td className="px-6 py-4 text-sm text-slate-300">
                          {shipment.destination || shipment.receiverName || "—"}
                        </td>

                        <td className="px-6 py-4">
                          <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs font-bold text-cyan-400">
                            {shipment.status || "CREATED"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

          </div>

        </section>
      </div>
    </main>
  );
}