"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export default function CreateShipmentPage() {
  const router = useRouter();

  const [senderName, setSenderName] = useState("");
  const [receiverName, setReceiverName] = useState("");
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [trackingNumber, setTrackingNumber] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    setLoading(true);
    setMessage("");
    setSuccess(false);

    try {
      const token = localStorage.getItem("token");

      if (!token) {
        setMessage("Please login first.");
        router.push("/login");
        return;
      }

      const response = await fetch(
        "http://localhost:8080/api/shipments",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            senderName,
            receiverName,
            origin,
            destination,
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
            `Shipment creation failed (${response.status})`
        );
      }

      setSuccess(true);
      setMessage("Shipment created successfully!");

      setTrackingNumber(data.trackingNumber || "");

      setSenderName("");
      setReceiverName("");
      setOrigin("");
      setDestination("");

    } catch (error) {
      console.error(error);

      setSuccess(false);

      setMessage(
        error instanceof Error
          ? error.message
          : "Unable to create shipment."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 px-4 py-10 text-white">

      {/* HEADER */}
      <header className="mx-auto mb-8 flex max-w-5xl items-center justify-between">
        <div>
          <h1 className="text-3xl font-extrabold">
            ShipTrack{" "}
            <span className="text-cyan-400">Pro</span>
          </h1>

          <p className="mt-1 text-sm text-slate-400">
            Create New Shipment
          </p>
        </div>

        <button
          onClick={() => router.push("/dashboard")}
          className="rounded-xl border border-slate-700 px-4 py-2 text-sm font-semibold transition hover:border-cyan-400 hover:text-cyan-400"
        >
          ← Dashboard
        </button>
      </header>

      {/* FORM CARD */}
      <section className="mx-auto max-w-5xl">

        <div className="overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 shadow-2xl">

          {/* TOP */}
          <div className="border-b border-slate-800 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 px-6 py-7 sm:px-10">

            <div className="flex items-center gap-4">

              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-500/10 text-3xl">
                📦
              </div>

              <div>
                <h2 className="text-2xl font-bold">
                  Shipment Details
                </h2>

                <p className="mt-1 text-sm text-slate-400">
                  Enter sender and receiver information
                </p>
              </div>

            </div>
          </div>

          {/* FORM */}
          <form
            onSubmit={handleSubmit}
            className="space-y-7 px-6 py-8 sm:px-10"
          >

            {/* SENDER / RECEIVER */}
            <div className="grid gap-6 md:grid-cols-2">

              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Sender Name
                </label>

                <input
                  type="text"
                  value={senderName}
                  onChange={(e) =>
                    setSenderName(e.target.value)
                  }
                  placeholder="Enter sender name"
                  required
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3.5 text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Receiver Name
                </label>

                <input
                  type="text"
                  value={receiverName}
                  onChange={(e) =>
                    setReceiverName(e.target.value)
                  }
                  placeholder="Enter receiver name"
                  required
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3.5 text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

            </div>

            {/* ORIGIN / DESTINATION */}
            <div className="grid gap-6 md:grid-cols-2">

              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Pickup / Origin
                </label>

                <input
                  type="text"
                  value={origin}
                  onChange={(e) =>
                    setOrigin(e.target.value)
                  }
                  placeholder="e.g. Indore, Madhya Pradesh"
                  required
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3.5 text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  Delivery / Destination
                </label>

                <input
                  type="text"
                  value={destination}
                  onChange={(e) =>
                    setDestination(e.target.value)
                  }
                  placeholder="e.g. Mumbai, Maharashtra"
                  required
                  className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3.5 text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-400 focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>

            </div>

            {/* INFO */}
            <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-5">

              <div className="flex gap-3">

                <span className="text-xl">
                  ℹ️
                </span>

                <div>
                  <h3 className="font-semibold text-blue-300">
                    Tracking Number
                  </h3>

                  <p className="mt-1 text-sm leading-6 text-slate-400">
                    A unique tracking number will be generated
                    automatically after creating the shipment.
                  </p>
                </div>

              </div>

            </div>

            {/* BUTTON */}
            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 px-6 py-4 font-bold shadow-lg shadow-cyan-500/10 transition hover:from-cyan-400 hover:to-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading
                ? "Creating Shipment..."
                : "Create Shipment"}
            </button>

          </form>

          {/* SUCCESS / ERROR */}
          {message && (
            <div className="px-6 pb-8 sm:px-10">

              <div
                className={`rounded-2xl border p-5 ${
                  success
                    ? "border-green-500/30 bg-green-500/10"
                    : "border-red-500/30 bg-red-500/10"
                }`}
              >

                <p
                  className={`font-semibold ${
                    success
                      ? "text-green-400"
                      : "text-red-400"
                  }`}
                >
                  {message}
                </p>

                {success && trackingNumber && (
                  <div className="mt-4 rounded-xl bg-slate-950 p-4">

                    <p className="text-xs uppercase tracking-widest text-slate-500">
                      Your Tracking Number
                    </p>

                    <p className="mt-1 text-2xl font-extrabold tracking-wider text-cyan-400">
                      {trackingNumber}
                    </p>

                  </div>
                )}

              </div>

            </div>
          )}

        </div>

      </section>
    </main>
  );
}