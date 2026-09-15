"use client";

import { useEffect, useState } from "react";
import Map from "@/components/Map";

type Shipment = {
  id: number;
  trackingNumber: string;
  senderName: string;
  receiverName: string;
  origin: string;
  destination: string;
  status: string;
};

const API_URL = "http://localhost:8080";

export default function TrackingPage() {
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
        const shipmentList = Array.isArray(data) ? data : [];

        setShipments(shipmentList);

        if (shipmentList.length > 0) {
          setSelectedShipment(shipmentList[0]);
        }
      })
      .catch((error) => {
        console.error("Tracking error:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const formatStatus = (status: string) => {
    return status
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "DELIVERED":
        return "text-green-400";

      case "IN_TRANSIT":
        return "text-blue-400";

      case "OUT_FOR_DELIVERY":
        return "text-yellow-400";

      case "CANCELLED":
        return "text-red-400";

      default:
        return "text-cyan-400";
    }
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
              Live Tracking
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

      {/* MAIN CONTENT */}
      <section className="mx-auto max-w-7xl px-5 py-8 sm:px-8">

        {/* INTRO */}
        <div className="mb-8">

          <p className="text-sm font-semibold uppercase tracking-widest text-cyan-400">
            Delivery Visibility
          </p>

          <h2 className="mt-2 text-3xl font-extrabold">
            Track Your Shipment
          </h2>

          <p className="mt-2 max-w-2xl text-slate-400">
            Monitor shipment status, route and current delivery location
            from one central tracking dashboard.
          </p>

        </div>

        {/* LOADING */}
        {loading ? (

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-400" />

            <p className="mt-4 text-sm text-slate-400">
              Loading shipments...
            </p>

          </div>

        ) : shipments.length === 0 ? (

          /* NO SHIPMENTS */
          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800 text-3xl">
              📦
            </div>

            <h3 className="mt-5 text-xl font-bold">
              No shipments available
            </h3>

            <p className="mt-2 text-slate-500">
              Create a shipment first to start tracking it.
            </p>

            <button
              onClick={() => {
                window.location.href = "/shipments";
              }}
              className="mt-6 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-3 text-sm font-bold transition hover:from-cyan-400 hover:to-blue-500"
            >
              Create Shipment
            </button>

          </div>

        ) : (

          /* TRACKING AREA */
          <div className="grid gap-6 lg:grid-cols-3">

            {/* SHIPMENT LIST */}
            <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

              <div className="mb-5">

                <h3 className="text-lg font-bold">
                  My Shipments
                </h3>

                <p className="mt-1 text-xs text-slate-500">
                  Select a shipment to track
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

                      <span className="text-lg">
                        📦
                      </span>

                    </div>

                    <p className="mt-2 truncate text-xs text-slate-500">
                      {shipment.origin}
                    </p>

                    <p className="mt-1 truncate text-xs text-slate-500">
                      ↓ {shipment.destination}
                    </p>

                    <p
                      className={`mt-3 text-xs font-semibold ${getStatusColor(
                        shipment.status
                      )}`}
                    >
                      {formatStatus(shipment.status)}
                    </p>

                  </button>

                ))}

              </div>

            </div>

            {/* DETAILS */}
            <div className="space-y-6 lg:col-span-2">

              {selectedShipment && (
                <>

                  {/* MAP */}
                  <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900">

                    <div className="border-b border-slate-800 px-6 py-5">

                      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">

                        <div>

                          <h3 className="text-lg font-bold">
                            Live Route Map
                          </h3>

                          <p className="mt-1 text-xs text-slate-500">
                            {selectedShipment.trackingNumber}
                          </p>

                        </div>

                        <span
                          className={`rounded-full bg-slate-800 px-3 py-1 text-xs font-semibold ${getStatusColor(
                            selectedShipment.status
                          )}`}
                        >
                          ● {formatStatus(selectedShipment.status)}
                        </span>

                      </div>

                    </div>

                    {/* ACTUAL OPENSTREETMAP */}
                    <div className="h-[420px] w-full">

                      <Map
                        origin={[28.6139, 77.2090]}
                        current={[26.9124, 75.7873]}
                        destination={[22.7196, 75.8577]}
                      />

                    </div>

                  </div>

                  {/* TRACKING INFORMATION */}
                  <div className="grid gap-5 sm:grid-cols-2">

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Tracking Number
                      </p>

                      <p className="mt-2 text-2xl font-extrabold text-cyan-400">
                        {selectedShipment.trackingNumber}
                      </p>

                    </div>

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Current Status
                      </p>

                      <p
                        className={`mt-2 text-2xl font-extrabold ${getStatusColor(
                          selectedShipment.status
                        )}`}
                      >
                        {formatStatus(selectedShipment.status)}
                      </p>

                    </div>

                  </div>

                  {/* SHIPMENT ROUTE */}
                  <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                    <h3 className="text-lg font-bold">
                      Shipment Route
                    </h3>

                    <p className="mt-1 text-sm text-slate-500">
                      Shipment origin and delivery destination
                    </p>

                    <div className="mt-6 grid gap-6 md:grid-cols-2">

                      {/* ROUTE TIMELINE */}
                      <div className="flex gap-4">

                        <div className="flex flex-col items-center">

                          <div className="h-4 w-4 rounded-full bg-cyan-400" />

                          <div className="h-16 w-px bg-slate-700" />

                          <div className="h-4 w-4 rounded-full bg-green-400" />

                        </div>

                        <div className="space-y-10">

                          <div>

                            <p className="text-xs font-semibold text-slate-500">
                              FROM
                            </p>

                            <p className="mt-1 font-semibold">
                              {selectedShipment.origin}
                            </p>

                          </div>

                          <div>

                            <p className="text-xs font-semibold text-slate-500">
                              TO
                            </p>

                            <p className="mt-1 font-semibold">
                              {selectedShipment.destination}
                            </p>

                          </div>

                        </div>

                      </div>

                      {/* PEOPLE */}
                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                        <p className="text-xs text-slate-500">
                          Receiver
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.receiverName}
                        </p>

                        <p className="mt-5 text-xs text-slate-500">
                          Sender
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.senderName}
                        </p>

                      </div>

                    </div>

                  </div>

                </>
              )}

            </div>

          </div>

        )}

      </section>

    </main>
  );
}