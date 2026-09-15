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

type Route = {
  id: number;
  origin: string;
  destination: string;
  distanceKm?: number | null;
  estimatedMinutes?: number | null;
  status: string;
  driver?: {
    id: number;
    fullName: string;
  } | null;
};

const API_URL = "http://localhost:8080";

export default function RoutesPage() {
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [selectedShipment, setSelectedShipment] =
    useState<Shipment | null>(null);

  const [route, setRoute] = useState<Route | null>(null);
  const [loading, setLoading] = useState(true);
  const [routeLoading, setRouteLoading] = useState(false);

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
        console.error("Shipment loading error:", error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    if (!selectedShipment) {
      return;
    }

    const token = localStorage.getItem("token");

    if (!token) {
      return;
    }

    setRouteLoading(true);
    setRoute(null);

    fetch(
      `${API_URL}/api/routes/${selectedShipment.id}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    )
      .then(async (response) => {
        if (response.status === 404) {
          return null;
        }

        if (!response.ok) {
          throw new Error("Failed to load route");
        }

        return response.json();
      })
      .then((data: Route | null) => {
        setRoute(data);
      })
      .catch((error) => {
        console.error("Route loading error:", error);
        setRoute(null);
      })
      .finally(() => {
        setRouteLoading(false);
      });
  }, [selectedShipment]);

  const formatMinutes = (minutes?: number | null) => {
    if (!minutes) {
      return "Not calculated";
    }

    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;

    if (hours === 0) {
      return `${remainingMinutes} min`;
    }

    if (remainingMinutes === 0) {
      return `${hours} hr`;
    }

    return `${hours} hr ${remainingMinutes} min`;
  };

  const formatDistance = (distance?: number | null) => {
    if (distance === null || distance === undefined) {
      return "Not calculated";
    }

    return `${distance.toFixed(1)} km`;
  };

  const createRoute = async () => {
    if (!selectedShipment) {
      return;
    }

    const token = localStorage.getItem("token");

    if (!token) {
      window.location.href = "/login";
      return;
    }

    setRouteLoading(true);

    try {
      const response = await fetch(
        `${API_URL}/api/routes`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            shipmentId: selectedShipment.id,
            origin: selectedShipment.origin,
            destination: selectedShipment.destination,
            status: "PLANNED",
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data?.message || "Unable to create route"
        );
      }

      setRoute(data);

    } catch (error) {
      console.error("Route creation error:", error);

      alert(
        error instanceof Error
          ? error.message
          : "Unable to create route"
      );
    } finally {
      setRouteLoading(false);
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
              Route Management
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

        {/* INTRO */}
        <div className="mb-8">

          <p className="text-sm font-semibold uppercase tracking-widest text-cyan-400">
            Logistics Management
          </p>

          <h2 className="mt-2 text-3xl font-extrabold">
            Shipment Routes
          </h2>

          <p className="mt-2 max-w-2xl text-slate-400">
            Plan routes, view distance and estimated travel time
            for your shipments.
          </p>

        </div>

        {loading ? (

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-400" />

            <p className="mt-4 text-sm text-slate-400">
              Loading shipments...
            </p>

          </div>

        ) : shipments.length === 0 ? (

          <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center">

            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-800 text-3xl">
              🚚
            </div>

            <h3 className="mt-5 text-xl font-bold">
              No shipments available
            </h3>

            <p className="mt-2 text-slate-500">
              Create a shipment before planning a route.
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

            {/* SHIPMENT SELECTOR */}
            <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

              <div className="mb-5">

                <h3 className="text-lg font-bold">
                  Select Shipment
                </h3>

                <p className="mt-1 text-xs text-slate-500">
                  Choose a shipment to view its route
                </p>

              </div>

              <div className="space-y-3">

                {shipments.map((shipment) => (

                  <button
                    key={shipment.id}
                    onClick={() => {
                      setSelectedShipment(shipment);
                    }}
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
                        🚚
                      </span>

                    </div>

                    <p className="mt-2 truncate text-xs text-slate-500">
                      {shipment.origin}
                    </p>

                    <p className="mt-1 truncate text-xs text-slate-500">
                      ↓ {shipment.destination}
                    </p>

                  </button>

                ))}

              </div>

            </div>

            {/* ROUTE DETAILS */}
            <div className="space-y-6 lg:col-span-2">

              {selectedShipment && (
                <>

                  {/* MAP */}
                  <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900">

                    <div className="border-b border-slate-800 px-6 py-5">

                      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">

                        <div>

                          <h3 className="text-lg font-bold">
                            Route Map
                          </h3>

                          <p className="mt-1 text-xs text-slate-500">
                            {selectedShipment.trackingNumber}
                          </p>

                        </div>

                        <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs font-semibold text-cyan-400">
                          {route ? "Route Available" : "Route Not Created"}
                        </span>

                      </div>

                    </div>

                    <div className="h-[420px] w-full">

                      <Map
                        origin={[28.6139, 77.2090]}
                        current={[26.9124, 75.7873]}
                        destination={[22.7196, 75.8577]}
                      />

                    </div>

                  </div>

                  {/* ROUTE INFORMATION */}
                  <div className="grid gap-4 sm:grid-cols-3">

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

                      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Distance
                      </p>

                      <p className="mt-2 text-2xl font-extrabold text-cyan-400">
                        {formatDistance(
                          route?.distanceKm
                        )}
                      </p>

                    </div>

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

                      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Estimated Time
                      </p>

                      <p className="mt-2 text-2xl font-extrabold text-blue-400">
                        {formatMinutes(
                          route?.estimatedMinutes
                        )}
                      </p>

                    </div>

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">

                      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Route Status
                      </p>

                      <p className="mt-2 text-2xl font-extrabold text-green-400">
                        {route?.status || "Not Planned"}
                      </p>

                    </div>

                  </div>

                  {/* ORIGIN / DESTINATION */}
                  <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                    <h3 className="text-lg font-bold">
                      Route Information
                    </h3>

                    <div className="mt-5 grid gap-4 md:grid-cols-2">

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Origin
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.origin}
                        </p>

                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                          Destination
                        </p>

                        <p className="mt-2 font-semibold">
                          {selectedShipment.destination}
                        </p>

                      </div>

                    </div>

                  </div>

                  {/* CREATE ROUTE */}
                  {!route && (

                    <div className="rounded-2xl border border-cyan-500/20 bg-cyan-500/5 p-6">

                      <div className="flex flex-col justify-between gap-5 md:flex-row md:items-center">

                        <div>

                          <h3 className="text-lg font-bold">
                            Create Route
                          </h3>

                          <p className="mt-1 text-sm text-slate-400">
                            Calculate route distance and estimated travel
                            time using OpenRouteService.
                          </p>

                        </div>

                        <button
                          onClick={createRoute}
                          disabled={routeLoading}
                          className="rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-3 text-sm font-bold transition hover:from-cyan-400 hover:to-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {routeLoading
                            ? "Calculating..."
                            : "Create Route →"}
                        </button>

                      </div>

                    </div>

                  )}

                  {/* DRIVER */}
                  {route && (

                    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6">

                      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">

                        <div>

                          <h3 className="text-lg font-bold">
                            Driver Assignment
                          </h3>

                          <p className="mt-1 text-sm text-slate-500">
                            Current driver assigned to this route
                          </p>

                        </div>

                        <div className="rounded-xl border border-slate-800 bg-slate-950 px-5 py-4">

                          {route.driver ? (

                            <>

                              <p className="text-xs text-slate-500">
                                Assigned Driver
                              </p>

                              <p className="mt-1 font-semibold">
                                {route.driver.fullName}
                              </p>

                            </>

                          ) : (

                            <p className="text-sm text-slate-500">
                              No driver assigned
                            </p>

                          )}

                        </div>

                      </div>

                    </div>

                  )}

                </>
              )}

            </div>

          </div>

        )}

      </section>

    </main>
  );
}