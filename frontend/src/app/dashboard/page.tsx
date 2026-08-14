"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { Activity, CheckCircle2, LogOut, Package, Plus, X } from "lucide-react";
import { Alert } from "@/components/Alert";
import { Brand } from "@/components/Brand";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth, getAuth } from "@/lib/auth";
import type {
  AuthSession,
  Shipment,
  ShipmentRequest,
  ShipmentStatus,
} from "@/lib/types";

const ALLOWED_TRANSITIONS: Record<ShipmentStatus, ShipmentStatus[]> = {
  CREATED: ["PICKED_UP", "CANCELLED"],
  PICKED_UP: ["IN_TRANSIT", "CANCELLED"],
  IN_TRANSIT: ["OUT_FOR_DELIVERY", "FAILED_DELIVERY", "CANCELLED"],
  OUT_FOR_DELIVERY: ["DELIVERED", "FAILED_DELIVERY", "CANCELLED"],
  FAILED_DELIVERY: ["OUT_FOR_DELIVERY", "CANCELLED"],
  DELIVERED: [],
  CANCELLED: [],
};

function label(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(value: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

export default function DashboardPage() {
  const router = useRouter();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState("");

  const canManageShipments = useMemo(
    () => session ? ["BUSINESS_CLIENT", "LOGISTICS_OPERATOR"].includes(session.user.role) : false,
    [session],
  );

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      const storedSession = getAuth();
      if (!storedSession?.token || !storedSession.user) {
        router.replace("/login");
        return;
      }

      setSession(storedSession);
      if (!["BUSINESS_CLIENT", "LOGISTICS_OPERATOR"].includes(storedSession.user.role)) {
        setLoading(false);
        return;
      }

      apiRequest<Shipment[]>("/shipments", {}, storedSession.token)
        .then((records) => {
          if (active) setShipments(records);
        })
        .catch((error: unknown) => {
          if (!active) return;
          if (error instanceof ApiError && error.status === 401) {
            clearAuth();
            router.replace("/login");
            return;
          }
          setMessage(error instanceof Error ? error.message : "Unable to load shipments.");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 0);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [router]);

  function handleApiFailure(error: unknown, fallback: string) {
    if (error instanceof ApiError && error.status === 401) {
      clearAuth();
      router.replace("/login");
      return;
    }
    setMessage(error instanceof Error ? error.message : fallback);
  }

  function logout() {
    clearAuth();
    router.replace("/login");
  }

  async function createShipment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const text = (name: string) => String(form.get(name) ?? "").trim();
    const payload: ShipmentRequest = {
      senderName: text("senderName"),
      senderPhone: text("senderPhone") || null,
      senderAddress: text("senderAddress"),
      receiverName: text("receiverName"),
      receiverPhone: text("receiverPhone") || null,
      receiverEmail: text("receiverEmail"),
      receiverAddress: text("receiverAddress"),
      pickupAddress: text("pickupAddress"),
      deliveryAddress: text("deliveryAddress"),
      priority: text("priority") as ShipmentRequest["priority"],
      packageDescription: text("packageDescription"),
      weightKg: Number(form.get("weightKg")),
      dimensions: text("dimensions"),
      quantity: Number(form.get("quantity")),
      declaredValue: Number(form.get("declaredValue")),
      fragile: form.get("fragile") === "on",
    };

    setMessage("");
    setSuccess("");
    setSaving(true);
    try {
      const created = await apiRequest<Shipment>("/shipments", {
        method: "POST",
        body: JSON.stringify(payload),
      }, session.token);
      setShipments((current) => [created, ...current]);
      formElement.reset();
      setShowCreate(false);
      setSuccess(`Shipment ${created.trackingNumber} created successfully.`);
    } catch (error) {
      handleApiFailure(error, "Unable to create shipment.");
    } finally {
      setSaving(false);
    }
  }

  async function updateStatus(shipment: Shipment, status: ShipmentStatus) {
    if (!session) return;
    const currentLocation = window.prompt(
      "Current location (optional)",
      shipment.currentLocation ?? "",
    );
    if (currentLocation === null) return;

    let cancellationReason: string | null = null;
    if (status === "CANCELLED") {
      cancellationReason = window.prompt("Cancellation reason (required)");
      if (!cancellationReason?.trim()) return;
    }

    setMessage("");
    setSuccess("");
    try {
      const updated = await apiRequest<Shipment>(`/shipments/${shipment.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          status,
          currentLocation: currentLocation.trim() || null,
          cancellationReason,
        }),
      }, session.token);
      setShipments((current) => current.map((item) => item.id === updated.id ? updated : item));
      setSuccess(`${updated.trackingNumber} moved to ${label(updated.status)}.`);
    } catch (error) {
      handleApiFailure(error, "Unable to update shipment status.");
    }
  }

  async function cancelShipment(shipment: Shipment) {
    if (!session) return;
    const reason = window.prompt("Why is this shipment being cancelled?");
    if (!reason?.trim()) return;

    setMessage("");
    setSuccess("");
    try {
      await apiRequest<void>(`/shipments/${shipment.id}?reason=${encodeURIComponent(reason.trim())}`, {
        method: "DELETE",
      }, session.token);
      setShipments((current) => current.map((item) =>
        item.id === shipment.id
          ? { ...item, status: "CANCELLED", cancellationReason: reason.trim() }
          : item,
      ));
      setSuccess(`${shipment.trackingNumber} cancelled.`);
    } catch (error) {
      handleApiFailure(error, "Unable to cancel shipment.");
    }
  }

  if (!session) {
    return <main className="loading-screen"><span className="spinner dark-spinner" />Loading your workspace...</main>;
  }

  const delivered = shipments.filter((shipment) => shipment.status === "DELIVERED").length;
  const active = shipments.filter((shipment) => !["DELIVERED", "CANCELLED"].includes(shipment.status)).length;
  const initials = session.user.fullName
    .split(" ")
    .slice(0, 2)
    .map((part) => part.charAt(0))
    .join("")
    .toUpperCase();

  return (
    <div className="dashboard-body">
      <header className="topbar">
        <div className="topbar-brand">
          <Brand light href="/dashboard" />
          <span className="workspace-chip">Operations</span>
        </div>
        <div className="topbar-user">
          <span className="user-avatar" aria-hidden="true">{initials}</span>
          <div className="user-copy"><strong>{session.user.fullName}</strong><span>{label(session.user.role)}</span></div>
          <button className="secondary-button signout-button" type="button" onClick={logout}><LogOut size={15} /> Sign out</button>
        </div>
      </header>

      <main className="dashboard-main">
        <section className="dashboard-heading">
          <div>
            <span className="eyebrow dark">Operations overview</span>
            <h1>Welcome back, {session.user.fullName.split(" ")[0]}</h1>
            <p>Monitor delivery progress and keep every hand-off moving.</p>
          </div>
          {canManageShipments && (
            <button className="primary-button action-button" type="button" onClick={() => setShowCreate((visible) => !visible)}>
              {showCreate ? <><X size={17} /> Close form</> : <><Plus size={17} /> Create shipment</>}
            </button>
          )}
        </section>

        <Alert message={message} />
        <Alert message={success} success />

        <section className="stats-grid" aria-label="Shipment summary">
          <article><div className="stat-top"><span>Total shipments</span><Package size={18} /></div><strong>{shipments.length}</strong><small>All recorded orders</small></article>
          <article><div className="stat-top"><span>Active delivery</span><Activity size={18} /></div><strong>{active}</strong><small>Currently in progress</small></article>
          <article><div className="stat-top"><span>Delivered</span><CheckCircle2 size={18} /></div><strong>{delivered}</strong><small>Successfully completed</small></article>
        </section>

        {!canManageShipments && (
          <section className="empty-card">
            <span className="empty-icon">i</span>
            <h2>Your account is active</h2>
            <p>Shipment management is available to business clients and logistics operators.</p>
          </section>
        )}

        {showCreate && canManageShipments && (
          <section className="create-card">
            <div className="section-heading">
              <div><span className="eyebrow dark">New order</span><h2>Create a shipment</h2></div>
              <p>All required information is sent securely to the Spring Boot API.</p>
            </div>
            <form onSubmit={createShipment}>
              <fieldset>
                <legend>Sender and pickup</legend>
                <div className="form-grid three-columns">
                  <div className="field"><label htmlFor="senderName">Sender name</label><input id="senderName" name="senderName" maxLength={120} required /></div>
                  <div className="field"><label htmlFor="senderPhone">Sender phone</label><input id="senderPhone" name="senderPhone" maxLength={25} /></div>
                  <div className="field wide-field"><label htmlFor="senderAddress">Sender address</label><input id="senderAddress" name="senderAddress" maxLength={500} required /></div>
                  <div className="field wide-field"><label htmlFor="pickupAddress">Pickup address</label><input id="pickupAddress" name="pickupAddress" maxLength={500} required /></div>
                </div>
              </fieldset>
              <fieldset>
                <legend>Receiver and delivery</legend>
                <div className="form-grid three-columns">
                  <div className="field"><label htmlFor="receiverName">Receiver name</label><input id="receiverName" name="receiverName" maxLength={120} required /></div>
                  <div className="field"><label htmlFor="receiverPhone">Receiver phone</label><input id="receiverPhone" name="receiverPhone" maxLength={25} /></div>
                  <div className="field"><label htmlFor="receiverEmail">Receiver email</label><input id="receiverEmail" name="receiverEmail" type="email" maxLength={254} required /></div>
                  <div className="field wide-field"><label htmlFor="receiverAddress">Receiver address</label><input id="receiverAddress" name="receiverAddress" maxLength={500} required /></div>
                  <div className="field wide-field"><label htmlFor="deliveryAddress">Delivery address</label><input id="deliveryAddress" name="deliveryAddress" maxLength={500} required /></div>
                </div>
              </fieldset>
              <fieldset>
                <legend>Package</legend>
                <div className="form-grid four-columns">
                  <div className="field span-two"><label htmlFor="packageDescription">Description</label><input id="packageDescription" name="packageDescription" maxLength={500} required /></div>
                  <div className="field"><label htmlFor="priority">Priority</label><select id="priority" name="priority" defaultValue="STANDARD"><option value="STANDARD">Standard</option><option value="EXPRESS">Express</option></select></div>
                  <div className="field"><label htmlFor="weightKg">Weight (kg)</label><input id="weightKg" name="weightKg" type="number" min="0.01" max="99999999.99" step="0.01" required /></div>
                  <div className="field"><label htmlFor="dimensions">Dimensions</label><input id="dimensions" name="dimensions" placeholder="30 × 20 × 15 cm" maxLength={100} required /></div>
                  <div className="field"><label htmlFor="quantity">Quantity</label><input id="quantity" name="quantity" type="number" min="1" step="1" defaultValue="1" required /></div>
                  <div className="field"><label htmlFor="declaredValue">Declared value (₹)</label><input id="declaredValue" name="declaredValue" type="number" min="0" step="0.01" required /></div>
                  <label className="check-card"><input name="fragile" type="checkbox" /><span><strong>Fragile shipment</strong><small>Requires careful handling</small></span></label>
                </div>
              </fieldset>
              <div className="form-actions">
                <button className="secondary-button" type="button" onClick={() => setShowCreate(false)}>Cancel</button>
                <button className="primary-button action-button" type="submit" disabled={saving}>{saving ? "Creating..." : <><Plus size={17} /> Create shipment</>}</button>
              </div>
            </form>
          </section>
        )}

        {canManageShipments && (
          <section className="shipments-section">
            <div className="section-heading compact">
              <div><span className="eyebrow dark">Live records</span><h2>Shipments</h2></div>
              <span className="record-count">{shipments.length} records</span>
            </div>

            {loading ? (
              <div className="empty-card"><span className="spinner dark-spinner" /><p>Loading shipments...</p></div>
            ) : shipments.length === 0 ? (
              <div className="empty-card"><span className="empty-icon">0</span><h3>No shipments yet</h3><p>Create your first shipment to start tracking delivery progress.</p></div>
            ) : (
              <div className="shipment-list">
                {shipments.map((shipment) => {
                  const transitions = ALLOWED_TRANSITIONS[shipment.status];
                  return (
                    <article className="shipment-card" key={shipment.id}>
                      <div className="shipment-main">
                        <div className="tracking-row">
                          <div><span className="tracking-label">Tracking number</span><strong>{shipment.trackingNumber}</strong></div>
                          <span className={`status-badge status-${shipment.status.toLowerCase()}`}>{label(shipment.status)}</span>
                        </div>
                        <div className="route-line" aria-label={`${shipment.pickupAddress} to ${shipment.deliveryAddress}`}>
                          <span className="route-dot" /><div><small>Pickup</small><strong>{shipment.pickupAddress}</strong></div>
                          <span className="route-track" />
                          <span className="route-dot destination" /><div><small>Delivery</small><strong>{shipment.deliveryAddress}</strong></div>
                        </div>
                        <div className="shipment-meta">
                          <div><small>Receiver</small><strong>{shipment.receiverName}</strong></div>
                          <div><small>Current location</small><strong>{shipment.currentLocation ?? "Not updated"}</strong></div>
                          <div><small>Estimated delivery</small><strong>{formatDate(shipment.estimatedDeliveryDate)}</strong></div>
                          <div><small>Priority</small><strong>{label(shipment.priority)}</strong></div>
                        </div>
                      </div>
                      <div className="shipment-actions">
                        {transitions.length > 0 ? (
                          <>
                            <label htmlFor={`status-${shipment.id}`}>Move to next status</label>
                            <select
                              id={`status-${shipment.id}`}
                              defaultValue=""
                              onChange={(event) => {
                                const status = event.target.value as ShipmentStatus;
                                event.target.value = "";
                                void updateStatus(shipment, status);
                              }}
                            >
                              <option value="" disabled>Select status</option>
                              {transitions.map((status) => <option value={status} key={status}>{label(status)}</option>)}
                            </select>
                            {shipment.status !== "CANCELLED" && shipment.status !== "DELIVERED" && (
                              <button className="danger-button" type="button" onClick={() => void cancelShipment(shipment)}>Cancel shipment</button>
                            )}
                          </>
                        ) : (
                          <span className="terminal-state">No further status actions</span>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        )}
      </main>
    </div>
  );
}
