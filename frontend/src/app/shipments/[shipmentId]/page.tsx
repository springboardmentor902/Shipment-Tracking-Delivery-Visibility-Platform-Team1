"use client";

import { ArrowLeft, Box, CalendarClock, Camera, CheckCircle2, FileSignature, LogOut, MapPinned, Package, Route as RouteIcon, ShieldCheck, Truck, UserRound, XCircle } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert } from "@/components/Alert";
import { Brand } from "@/components/Brand";
import { EtaWidget } from "@/components/EtaWidget";
import { NotificationBell } from "@/components/NotificationBell";
import { ProtectedImage } from "@/components/ProtectedImage";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth, getAuth } from "@/lib/auth";
import type { AuthSession, DeliveryRoute, ProofOfDelivery, ProofVerificationStatus, Shipment } from "@/lib/types";

function label(value: string) {
  return value.toLowerCase().split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
}

function formatDateTime(value: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatDuration(minutes: number | null) {
  if (minutes === null) return "Pending";
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  if (!hours) return `${minutes} min`;
  return remainder ? `${hours} hr ${remainder} min` : `${hours} hr`;
}

export default function ShipmentDetailPage() {
  const params = useParams<{ shipmentId: string }>();
  const router = useRouter();
  const shipmentId = Number(params.shipmentId);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [route, setRoute] = useState<DeliveryRoute | null>(null);
  const [proof, setProof] = useState<ProofOfDelivery | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

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

      const shipmentRequest = apiRequest<Shipment>(`/shipments/${shipmentId}`, {}, storedSession.token);
      const routeRequest = apiRequest<DeliveryRoute>(`/routes/${shipmentId}`, {}, storedSession.token)
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.status === 404) return null;
          throw error;
        });
      const proofRequest = apiRequest<ProofOfDelivery>(`/pod/${shipmentId}`, {}, storedSession.token)
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.status === 404) return null;
          throw error;
        });
      Promise.all([shipmentRequest, routeRequest, proofRequest])
        .then(([shipmentRecord, routeRecord, proofRecord]) => {
          if (!active) return;
          setShipment(shipmentRecord);
          setRoute(routeRecord);
          setProof(proofRecord);
        })
        .catch((error: unknown) => {
          if (!active) return;
          if (error instanceof ApiError && error.status === 401) {
            clearAuth();
            router.replace("/login");
            return;
          }
          setMessage(error instanceof Error ? error.message : "Shipment details could not be loaded.");
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

  function logout() {
    clearAuth();
    router.replace("/login");
  }

  async function verifyProof(status: Exclude<ProofVerificationStatus, "PENDING">) {
    if (!session || !proof) return;
    const notes = window.prompt(status === "VERIFIED" ? "Verification notes (optional)" : "Reason for rejection");
    if (notes === null) return;
    setMessage("");
    try {
      const updated = await apiRequest<ProofOfDelivery>(`/pod/${proof.shipmentId}/verify`, {
        method: "PATCH",
        body: JSON.stringify({ status, notes: notes.trim() || null }),
      }, session.token);
      setProof(updated);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Proof verification could not be saved.");
    }
  }

  if (!session || loading) {
    return <main className="loading-screen"><span className="spinner dark-spinner" />Opening shipment details...</main>;
  }

  const canRecalculate = ["LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role);
  const canVerifyProof = ["SUPPORT_AGENT", "ADMINISTRATOR"].includes(session.user.role);
  const initials = session.user.fullName.split(" ").slice(0, 2).map((part) => part.charAt(0)).join("").toUpperCase();

  return (
    <div className="dashboard-body detail-page-body">
      <header className="topbar">
        <div className="topbar-brand"><Brand light href="/dashboard" /><span className="workspace-chip">Shipment detail</span></div>
        <div className="topbar-user">
          <NotificationBell token={session.token} />
          <span className="user-avatar" aria-hidden="true">{initials}</span>
          <div className="user-copy"><strong>{session.user.fullName}</strong><span>{label(session.user.role)}</span></div>
          <button className="secondary-button signout-button" type="button" onClick={logout}><LogOut size={15} /> Sign out</button>
        </div>
      </header>

      <main className="tracking-main detail-main">
        <Link className="tracking-back-link" href="/dashboard"><ArrowLeft size={14} /> Back to operations</Link>
        <Alert message={message} />
        {!shipment ? (
          <section className="empty-card"><Package size={26} /><h2>Shipment unavailable</h2><p>The record could not be opened with this account.</p></section>
        ) : (
          <>
            <section className="detail-hero">
              <div>
                <span className="eyebrow dark">Shipment record</span>
                <h1>{shipment.trackingNumber}</h1>
                <p>Created {formatDate(shipment.createdAt)} · {shipment.packages.length} package{shipment.packages.length === 1 ? "" : "s"}</p>
              </div>
              <div className="detail-hero-actions">
                <span className={`status-badge status-${shipment.status.toLowerCase()}`}>{label(shipment.status)}</span>
                {route && <Link className="primary-button detail-live-link" href={`/tracking/${shipment.id}`}><MapPinned size={15} /> Live tracking</Link>}
              </div>
            </section>

            <div className="detail-layout">
              <div className="detail-content-column">
                <section className="detail-surface">
                  <div className="detail-section-title"><RouteIcon size={16} /><div><strong>Delivery journey</strong><small>Current route and promise</small></div></div>
                  <div className="detail-route-line">
                    <div><i className="origin" /><small>Pickup</small><strong>{shipment.pickupAddress}</strong></div>
                    <span />
                    <div><i className="destination" /><small>Delivery</small><strong>{shipment.deliveryAddress}</strong></div>
                  </div>
                  <div className="detail-data-grid">
                    <div><CalendarClock size={14} /><small>Original estimate</small><strong>{formatDate(shipment.estimatedDeliveryDate)}</strong></div>
                    <div><MapPinned size={14} /><small>Current location</small><strong>{shipment.currentLocation ?? "Not updated"}</strong></div>
                    <div><Truck size={14} /><small>Travel time</small><strong>{route ? formatDuration(route.estimatedTimeMinutes) : "Route pending"}</strong></div>
                    <div><RouteIcon size={14} /><small>Traffic</small><strong>{route ? label(route.trafficCondition) : "Route pending"}</strong></div>
                  </div>
                </section>

                <section className="detail-surface">
                  <div className="detail-section-title"><UserRound size={16} /><div><strong>Delivery contacts</strong><small>Sender, receiver and driver</small></div></div>
                  <dl className="detail-contact-grid">
                    <div><dt>Sender</dt><dd>{shipment.senderName}</dd><small>{shipment.senderPhone ?? "Phone not provided"}</small></div>
                    <div><dt>Receiver</dt><dd>{shipment.receiverName}</dd><small>{shipment.receiverEmail}</small></div>
                    <div><dt>Assigned driver</dt><dd>{route?.driverName ?? "Not assigned"}</dd><small>{route?.vehicleNumber ?? "Vehicle pending"}</small></div>
                  </dl>
                </section>

                {proof && (
                  <section className="detail-surface proof-detail-card">
                    <div className="detail-section-title proof-title-row">
                      <FileSignature size={16} />
                      <div><strong>Proof of delivery</strong><small>Submitted {formatDateTime(proof.submittedAt)}</small></div>
                      <span className={`proof-status proof-${proof.verificationStatus.toLowerCase()}`}>{label(proof.verificationStatus)}</span>
                    </div>
                    <div className="proof-summary-grid">
                      <div><small>Received by</small><strong>{proof.recipientName}</strong></div>
                      <div><small>Submitted by</small><strong>{proof.submittedBy}</strong></div>
                      <div className="proof-notes"><small>Delivery notes</small><strong>{proof.deliveryNotes ?? "No notes added"}</strong></div>
                    </div>
                    <div className="proof-images">
                      <figure>
                        <ProtectedImage src={proof.signatureUrl} alt={`Signature of ${proof.recipientName}`} token={session.token} />
                        <figcaption><FileSignature size={12} /> Recipient signature</figcaption>
                      </figure>
                      <figure>
                        <ProtectedImage src={proof.photoUrl} alt="Delivery evidence" token={session.token} />
                        <figcaption><Camera size={12} /> Delivery photo</figcaption>
                      </figure>
                    </div>
                    {proof.verifiedBy && (
                      <p className="proof-verification-copy"><ShieldCheck size={13} /> {label(proof.verificationStatus)} by {proof.verifiedBy} on {formatDateTime(proof.verifiedAt)}{proof.verificationNotes ? ` · ${proof.verificationNotes}` : ""}</p>
                    )}
                    {canVerifyProof && proof.verificationStatus === "PENDING" && (
                      <div className="proof-verification-actions">
                        <button className="secondary-button" type="button" onClick={() => void verifyProof("REJECTED")}><XCircle size={14} /> Reject</button>
                        <button className="primary-button" type="button" onClick={() => void verifyProof("VERIFIED")}><CheckCircle2 size={14} /> Verify proof</button>
                      </div>
                    )}
                  </section>
                )}

                <section className="detail-surface">
                  <div className="detail-section-title"><Box size={16} /><div><strong>Package manifest</strong><small>Items linked to shipment #{shipment.id}</small></div></div>
                  <div className="detail-package-list">
                    {shipment.packages.map((item, index) => (
                      <article key={item.id}>
                        <span>{String(index + 1).padStart(2, "0")}</span>
                        <div><strong>{item.description}</strong><small>{item.quantity} unit(s) · {item.weightKg} kg · {item.dimensions}</small></div>
                        <em>{item.fragile ? "Fragile" : "Standard"}</em>
                      </article>
                    ))}
                  </div>
                </section>
              </div>

              <aside className="detail-sidebar">
                <EtaWidget shipmentId={shipment.id} token={session.token} canRecalculate={canRecalculate && Boolean(route)} />
                <section className="detail-surface detail-route-card">
                  <div className="detail-section-title"><Truck size={16} /><div><strong>Route summary</strong><small>{route ? `Route #${route.id}` : "Awaiting route"}</small></div></div>
                  {route ? (
                    <dl className="driver-detail-list">
                      <div><dt>Distance</dt><dd>{route.distanceKm === null ? "Pending" : `${Number(route.distanceKm).toFixed(2)} km`}</dd></div>
                      <div><dt>Driver</dt><dd>{route.driverName ?? "Not assigned"}</dd></div>
                      <div><dt>Vehicle</dt><dd>{route.vehicleNumber ?? "Not assigned"}</dd></div>
                      <div><dt>Traffic</dt><dd>{label(route.trafficCondition)}</dd></div>
                    </dl>
                  ) : <p className="operation-empty">An operator needs to create the route before live tracking and ETA calculation begin.</p>}
                </section>
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
