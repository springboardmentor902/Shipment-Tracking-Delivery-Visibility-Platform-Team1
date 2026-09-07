"use client";

import { Client, type StompSubscription } from "@stomp/stompjs";
import {
  ArrowLeft,
  Clock3,
  MapPin,
  Navigation,
  Radio,
  Route as RouteIcon,
  Send,
  Truck,
} from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/Alert";
import { AppHeader } from "@/components/AppHeader";
import { EtaWidget } from "@/components/EtaWidget";
import { LiveTrackingMap } from "@/components/LiveTrackingMap";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth, getAuth } from "@/lib/auth";
import type { AuthSession, DeliveryRoute, LocationUpdate, Shipment } from "@/lib/types";

type ConnectionStatus = "connecting" | "connected" | "reconnecting" | "offline";

function formatDateTime(value: string | null) {
  if (!value) return "Awaiting first driver update";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(value));
}

function formatDuration(minutes: number | null) {
  if (minutes === null) return "Pending";
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours} hr ${remainder} min` : `${hours} hr`;
}

function websocketUrl() {
  const configured = process.env.NEXT_PUBLIC_WEBSOCKET_URL?.trim();
  if (configured) return configured;
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.hostname}:8080/api/ws/tracking`;
}

export default function TrackingPage() {
  const params = useParams<{ shipmentId: string }>();
  const router = useRouter();
  const shipmentId = Number(params.shipmentId);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [route, setRoute] = useState<DeliveryRoute | null>(null);
  const [currentLocation, setCurrentLocation] = useState<LocationUpdate | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("connecting");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      const storedSession = getAuth();
      if (!storedSession?.token || !storedSession.user) {
        router.replace("/login");
        return;
      }
      if (!Number.isInteger(shipmentId) || shipmentId <= 0) {
        router.replace("/dashboard");
        return;
      }

      setSession(storedSession);
      Promise.all([
        apiRequest<Shipment>(`/shipments/${shipmentId}`, {}, storedSession.token),
        apiRequest<DeliveryRoute>(`/routes/${shipmentId}`, {}, storedSession.token),
      ])
        .then(([shipmentRecord, routeRecord]) => {
          if (!active) return;
          setShipment(shipmentRecord);
          setRoute(routeRecord);
          if (routeRecord.lastKnownLatitude !== null && routeRecord.lastKnownLongitude !== null) {
            setCurrentLocation({
              routeId: routeRecord.id,
              shipmentId: routeRecord.shipmentId,
              trackingNumber: routeRecord.trackingNumber,
              latitude: Number(routeRecord.lastKnownLatitude),
              longitude: Number(routeRecord.lastKnownLongitude),
              recordedAt: routeRecord.lastLocationUpdatedAt ?? routeRecord.updatedAt,
            });
          }
        })
        .catch((error: unknown) => {
          if (!active) return;
          if (error instanceof ApiError && error.status === 401) {
            clearAuth();
            router.replace("/login");
            return;
          }
          setMessage(error instanceof Error ? error.message : "Unable to open live tracking.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 0);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [router, shipmentId]);

  useEffect(() => {
    if (!session || !route) return;
    let disposed = false;
    let subscription: StompSubscription | null = null;

    const client = new Client({
      brokerURL: websocketUrl(),
      connectHeaders: { Authorization: `Bearer ${session.token}` },
      reconnectDelay: 4000,
      connectionTimeout: 8000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
    });

    client.onConnect = () => {
      if (disposed) return;
      setConnectionStatus("connected");
      setMessage("");
      subscription = client.subscribe(
        `/topic/shipments/${shipmentId}/location`,
        (frame) => {
          try {
            const update = JSON.parse(frame.body) as LocationUpdate;
            if (update.shipmentId === shipmentId) {
              setCurrentLocation({
                ...update,
                latitude: Number(update.latitude),
                longitude: Number(update.longitude),
              });
            }
          } catch {
            setMessage("A live location message could not be read.");
          }
        },
      );
    };
    client.onWebSocketClose = () => {
      if (!disposed) setConnectionStatus("reconnecting");
    };
    client.onWebSocketError = () => {
      if (!disposed) setConnectionStatus("reconnecting");
    };
    client.onStompError = (frame) => {
      if (!disposed) {
        setConnectionStatus("offline");
        setMessage(frame.headers.message || "Live tracking authorization failed.");
      }
    };

    client.activate();

    return () => {
      disposed = true;
      subscription?.unsubscribe();
      void client.deactivate();
    };
  }, [route, session, shipmentId]);

  const origin = useMemo(() => route?.originLatitude !== null && route?.originLongitude !== null && route
    ? { latitude: Number(route.originLatitude), longitude: Number(route.originLongitude) }
    : null, [route]);
  const destination = useMemo(() => route?.destinationLatitude !== null && route?.destinationLongitude !== null && route
    ? { latitude: Number(route.destinationLatitude), longitude: Number(route.destinationLongitude) }
    : null, [route]);
  const liveCoordinates = useMemo(() => currentLocation
    ? { latitude: currentLocation.latitude, longitude: currentLocation.longitude }
    : null, [currentLocation]);

  function handleApiFailure(error: unknown, fallback: string) {
    if (error instanceof ApiError && error.status === 401) {
      clearAuth();
      router.replace("/login");
      return;
    }
    setMessage(error instanceof Error ? error.message : fallback);
  }

  async function sendLocation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !route) return;
    const form = new FormData(event.currentTarget);
    const latitude = Number(form.get("latitude"));
    const longitude = Number(form.get("longitude"));

    setMessage("");
    setSuccess("");
    setSending(true);
    try {
      const update = await apiRequest<LocationUpdate>(`/route/${route.id}/location`, {
        method: "POST",
        body: JSON.stringify({ latitude, longitude }),
      }, session.token);
      setCurrentLocation({
        ...update,
        latitude: Number(update.latitude),
        longitude: Number(update.longitude),
      });
      setSuccess("Driver location saved and broadcast to subscribed customers.");
    } catch (error) {
      handleApiFailure(error, "Unable to send the driver location.");
    } finally {
      setSending(false);
    }
  }

  function logout() {
    clearAuth();
    router.replace("/login");
  }

  if (!session || loading) {
    return <main className="loading-screen"><span className="spinner dark-spinner" />Opening live tracking...</main>;
  }

  const canSendLocation = ["LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role);
  return (
    <div className="dashboard-body tracking-page-body">
      <AppHeader session={session} section="Live tracking" onSignOut={logout} />

      <main className="tracking-main">
        <Link className="tracking-back-link" href="/dashboard"><ArrowLeft size={14} /> Back to operations</Link>
        <section className="tracking-heading">
          <div>
            <span className="eyebrow dark">Live tracking</span>
            <h1>{shipment?.trackingNumber ?? "Live shipment tracking"}</h1>
            <p>{shipment ? `${shipment.pickupAddress} to ${shipment.deliveryAddress}` : "The shipment could not be loaded."}</p>
          </div>
          <span className={`socket-status socket-${connectionStatus}`}><i />{connectionStatus === "connected" ? "Live connection" : connectionStatus}</span>
        </section>

        <Alert message={message} />
        <Alert message={success} success />

        {!shipment || !route ? (
          <section className="empty-card tracking-empty"><MapPin size={25} /><h2>Tracking is unavailable</h2><p>Create a route for this shipment before opening live tracking.</p></section>
        ) : (
          <div className="tracking-layout">
            <section className="tracking-map-card">
              <div className="tracking-card-heading">
                <div><span className="eyebrow dark">Map</span><h2>Driver location</h2></div>
                <span className="map-live-chip"><Radio size={12} /> Live</span>
              </div>
              <LiveTrackingMap currentLocation={liveCoordinates} origin={origin} destination={destination} />
              <div className="map-status-strip">
                <div><span className="map-strip-icon"><Navigation size={15} /></span><small>Current coordinates</small><strong>{currentLocation ? `${currentLocation.latitude.toFixed(6)}, ${currentLocation.longitude.toFixed(6)}` : "Awaiting driver ping"}</strong></div>
                <div><span className="map-strip-icon"><Clock3 size={15} /></span><small>Last received</small><strong>{formatDateTime(currentLocation?.recordedAt ?? null)}</strong></div>
              </div>
            </section>

            <aside className="tracking-sidebar">
              <EtaWidget
                shipmentId={shipment.id}
                token={session.token}
                canRecalculate={canSendLocation}
                refreshKey={currentLocation?.recordedAt ?? null}
              />
              <section className="tracking-info-card">
                <div className="operation-title"><span className="operation-icon"><RouteIcon size={16} /></span><div><strong>Planned route</strong><small>Route #{route.id}</small></div></div>
                <div className="tracking-route-points">
                  <div><span className="tracking-point origin" /><small>Origin</small><strong>{route.originAddress}</strong></div>
                  <span className="tracking-point-line" />
                  <div><span className="tracking-point destination" /><small>Destination</small><strong>{route.destinationAddress}</strong></div>
                </div>
                <div className="tracking-info-grid">
                  <div><small>Distance</small><strong>{route.distanceKm === null ? "Pending" : `${Number(route.distanceKm).toFixed(2)} km`}</strong></div>
                  <div><small>Travel time</small><strong>{formatDuration(route.estimatedTimeMinutes)}</strong></div>
                </div>
              </section>

              <section className="tracking-info-card">
                <div className="operation-title"><span className="operation-icon"><Truck size={16} /></span><div><strong>Delivery vehicle</strong><small>Assigned route resource</small></div></div>
                <dl className="driver-detail-list">
                  <div><dt>Driver</dt><dd>{route.driverName ?? "Not assigned"}</dd></div>
                  <div><dt>Phone</dt><dd>{route.driverPhone ?? "Not available"}</dd></div>
                  <div><dt>Vehicle</dt><dd>{route.vehicleNumber ?? "Not assigned"}</dd></div>
                </dl>
              </section>

              {canSendLocation && (
                <section className="tracking-info-card driver-location-card">
                  <div className="operation-title"><span className="operation-icon"><Radio size={16} /></span><div><strong>Update driver location</strong><small>Enter the current coordinates</small></div></div>
                  <form onSubmit={sendLocation}>
                    <div className="field"><label htmlFor="driver-latitude">Latitude</label><input id="driver-latitude" name="latitude" type="number" min="-90" max="90" step="0.0000001" defaultValue={currentLocation?.latitude ?? route.originLatitude ?? ""} required /></div>
                    <div className="field"><label htmlFor="driver-longitude">Longitude</label><input id="driver-longitude" name="longitude" type="number" min="-180" max="180" step="0.0000001" defaultValue={currentLocation?.longitude ?? route.originLongitude ?? ""} required /></div>
                    <button className="primary-button driver-location-submit" type="submit" disabled={sending}><Send size={14} />{sending ? "Updating..." : "Update location"}</button>
                  </form>
                </section>
              )}
            </aside>
          </div>
        )}
      </main>
    </div>
  );
}
