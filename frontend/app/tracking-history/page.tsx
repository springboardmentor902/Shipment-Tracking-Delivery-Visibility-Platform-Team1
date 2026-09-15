"use client";

import { useEffect, useState } from "react";

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

const API_URL = "http://localhost:8080";

const statuses = [
  {
    key: "CREATED",
    title: "Shipment Created",
    description: "Shipment has been created successfully.",
    icon: "📦",
  },
  {
    key: "PICKED_UP",
    title: "Picked Up",
    description: "Package has been picked up from the sender.",
    icon: "🤝",
  },
  {
    key: "IN_TRANSIT",
    title: "In Transit",
    description: "Shipment is moving towards the destination.",
    icon: "🚚",
  },
  {
    key: "OUT_FOR_DELIVERY",
    title: "Out for Delivery",
    description: "Shipment is with the delivery agent.",
    icon: "🛵",
  },
  {
    key: "DELIVERED",
    title: "Delivered",
    description: "Shipment has been successfully delivered.",
    icon: "✅",
  },
];

export default function TrackingHistoryPage() {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [selectedShipment, setSelectedShipment] =
    useState<Shipment | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("token");

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
        const list = Array.isArray(data) ? data : [];

        setShipments(list);

        if (list.length > 0) {
          setSelectedShipment(list[0]);
        }
      })
      .catch((error) => {
        console.error("History loading error:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const getCurrentStep = (status: string) => {
    const index = statuses.findIndex(
      (item) => item.key === status
    );

    return index >= 0 ? index : 0;
  };

  const formatStatus = (status: string) => {
    return status
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  return (
    <main className="min-h-screen bg-slate-950 text-white">

      {/* HEADER */}
      <header className="border-b border-slate-800 bg-slate-900">

        <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-5 sm:px-8">

          <div>

            <p className="text-xs font-semibold uppercase tracking-widest text-cyan-400">
              ShipTrack Pro
            </p>

            <h1 className="mt-1 text-2xl font-extrabold">
              Tracking History
            </h1>

          </div>

          <button
            onClick={() => {
              window.location.href = "/dashboard";
            }}
            className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:bg-slate-800 hover:text-white"
          >
            ← Dashboard
          </button>

        </div>

      </header>

      {/* CONTENT */}
      <section className="mx-auto max-w-7xl px-5 py-8 sm:px-8">

        <div className="mb-8">

          <p className="text-sm font-semibold uppercase tracking-widest text-cyan-400">
            Shipment Activity
          </p>

          <h2 className="mt-2 text-3xl font-extrabold">
            Tracking History
          </h2>

          <p className="mt-2 text-slate-400">
            View the complete delivery lifecycle of your shipments.
          </p>

        </div>

        {/* LOADING */}
        {loading ? (

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-400" />

            <p className="mt-4 text-sm text-slate-400">
              Loading shipment history...
            </p>

          </div>

        ) : shipments.length === 0 ? (

          /* EMPTY */
          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800 text-3xl">
              🕒
            </div>

            <h3 className="mt-5 text-xl font-bold">
              No shipment history
            </h3>

            <p className="mt-2 text-slate-500">
              Create a shipment to start tracking its lifecycle.
            </p>

            <button
              onClick={() => {
                window.location.href = "/shipments";
              }}
              className="mt-6 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-3 text-sm font-bold"
            >
              Create Shipment
            </button>

          </div>

        ) : (

          <div className="grid gap-6 lg:grid-cols-3">

            {/* SHIPMENT LIST */}
            <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

              <div className="mb-5">

                <h3 className="text-lg font-bold">
                  Shipments
                </h3>

                <p className="mt-1 text-xs text-slate-500">
                  Select a shipment
                </p>

              </div>

              <div className="space-y-3">

                {shipments.map((shipment) => (

                  <button
                    key={shipment.id}
                    onClick={() => setSelectedShipment(shipment)}
                    className={`w-full rounded-xl border p-4 text-left transition ${
                      selectedShipment?.id === shipment.id
                        ? "border-cyan-500/50 bg-cyan-500/10"
                        : "border-slate-800 bg-slate-950 hover:border-slate-700"
                    }`}
                  >

                    <div className="flex items-center justify-between">

                      <span className="font-bold">
                        {shipment.trackingNumber}
                      </span>

                      <span>
                        📦
                      </span>

                    </div>

                    <p className="mt-2 truncate text-xs text-slate-500">
                      {shipment.origin}
                    </p>

                    <p className="mt-1 truncate text-xs text-slate-500">
                      ↓ {shipment.destination}
                    </p>

                    <p className="mt-3 text-xs font-semibold text-cyan-400">
                      {formatStatus(shipment.status)}
                    </p>

                  </button>

                ))}

              </div>

            </div>

            {/* HISTORY */}
            <div className="lg:col-span-2">

              {selectedShipment && (
                <div className="rounded-2xl border border-slate-800 bg-slate-900">

                  {/* SHIPMENT HEADER */}
                  <div className="border-b border-slate-800 p-6">

                    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">

                      <div>

                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Tracking Number
                        </p>

                        <h3 className="mt-1 text-2xl font-extrabold text-cyan-400">
                          {selectedShipment.trackingNumber}
                        </h3>

                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950 px-4 py-3">

                        <p className="text-xs text-slate-500">
                          Current Status
                        </p>

                        <p className="mt-1 font-bold text-cyan-400">
                          {formatStatus(selectedShipment.status)}
                        </p>

                      </div>

                    </div>

                  </div>

                  {/* TIMELINE */}
                  <div className="p-6">

                    <h3 className="text-lg font-bold">
                      Shipment Timeline
                    </h3>

                    <p className="mt-1 text-sm text-slate-500">
                      Delivery lifecycle
                    </p>

                    <div className="mt-8">

                      {statuses.map((status, index) => {

                        const currentStep =
                          getCurrentStep(
                            selectedShipment.status
                          );

                        const completed =
                          index <= currentStep;

                        const active =
                          index === currentStep;

                        return (
                          <div
                            key={status.key}
                            className="relative flex gap-5"
                          >

                            {/* CONNECTING LINE */}
                            {index < statuses.length - 1 && (
                              <div
                                className={`absolute left-[19px] top-10 h-20 w-0.5 ${
                                  index < currentStep
                                    ? "bg-cyan-400"
                                    : "bg-slate-700"
                                }`}
                              />
                            )}

                            {/* ICON */}
                            <div
                              className={`relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full border text-lg ${
                                completed
                                  ? "border-cyan-400/50 bg-cyan-500/10"
                                  : "border-slate-700 bg-slate-950"
                              }`}
                            >
                              {completed
                                ? status.icon
                                : "○"}
                            </div>

                            {/* CONTENT */}
                            <div className="pb-12">

                              <div className="flex flex-wrap items-center gap-3">

                                <h4
                                  className={`font-bold ${
                                    completed
                                      ? "text-white"
                                      : "text-slate-500"
                                  }`}
                                >
                                  {status.title}
                                </h4>

                                {active && (
                                  <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs font-semibold text-cyan-400">
                                    Current
                                  </span>
                                )}

                              </div>

                              <p
                                className={`mt-1 text-sm ${
                                  completed
                                    ? "text-slate-400"
                                    : "text-slate-600"
                                }`}
                              >
                                {status.description}
                              </p>

                              {completed && (
                                <p className="mt-2 text-xs text-slate-600">
                                  Shipment status updated
                                </p>
                              )}

                            </div>

                          </div>
                        );
                      })}

                    </div>

                  </div>

                  {/* ROUTE SUMMARY */}
                  <div className="border-t border-slate-800 p-6">

                    <h3 className="text-lg font-bold">
                      Route Summary
                    </h3>

                    <div className="mt-5 grid gap-4 sm:grid-cols-2">

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                        <p className="text-xs text-slate-500">
                          Origin
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.origin}
                        </p>

                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                        <p className="text-xs text-slate-500">
                          Destination
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.destination}
                        </p>

                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                        <p className="text-xs text-slate-500">
                          Sender
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.senderName}
                        </p>

                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">

                        <p className="text-xs text-slate-500">
                          Receiver
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.receiverName}
                        </p>

                      </div>

                    </div>

                  </div>

                </div>
              )}

            </div>

          </div>

        )}

      </section>

    </main>
  );
}