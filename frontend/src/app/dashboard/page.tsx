"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  CheckCircle2,
  ChevronDown,
  LogOut,
  MapPinned,
  Navigation,
  Package,
  Plus,
  Route as RouteIcon,
  Trash2,
  Truck,
  UserRoundCheck,
  X,
} from "lucide-react";
import { Alert } from "@/components/Alert";
import { Brand } from "@/components/Brand";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth, getAuth } from "@/lib/auth";
import type {
  AuthSession,
  DeliveryRoute,
  DriverAssignmentRequest,
  RouteRequest,
  Shipment,
  ShipmentRequest,
  ShipmentStatus,
  User,
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

function formatCoordinate(value: number | null) {
  return value === null ? "—" : Number(value).toFixed(5);
}

function formatDuration(minutes: number | null) {
  if (minutes === null) return "Pending";
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return remainder ? `${hours} hr ${remainder} min` : `${hours} hr`;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(value);
}

export default function DashboardPage() {
  const router = useRouter();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [operators, setOperators] = useState<User[]>([]);
  const [routes, setRoutes] = useState<Record<number, DeliveryRoute | null>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [expandedShipmentId, setExpandedShipmentId] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState("");
  const [packageRows, setPackageRows] = useState([0]);
  const nextPackageId = useRef(1);

  const canViewShipments = useMemo(
    () => session ? ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role) : false,
    [session],
  );
  const canCreateShipments = useMemo(
    () => session ? ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role) : false,
    [session],
  );
  const canUpdateShipmentStatus = useMemo(
    () => session ? ["LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role) : false,
    [session],
  );
  const canManageRoutes = useMemo(
    () => session ? ["LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(session.user.role) : false,
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
      if (!["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR"].includes(storedSession.user.role)) {
        setLoading(false);
        return;
      }

      const shipmentRequest = apiRequest<Shipment[]>("/shipments", {}, storedSession.token);
      const operatorRequest = storedSession.user.role === "ADMINISTRATOR"
        ? apiRequest<User[]>("/admin/users", {}, storedSession.token)
        : Promise.resolve([]);

      Promise.all([shipmentRequest, operatorRequest])
        .then(([records, users]) => {
          if (!active) return;
          setShipments(records);
          setOperators(users.filter((user) => user.role === "LOGISTICS_OPERATOR" && user.status === "ACTIVE"));
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

  function addPackageRow() {
    const rowId = nextPackageId.current++;
    setPackageRows((current) => [...current, rowId]);
  }

  function removePackageRow(rowId: number) {
    setPackageRows((current) => current.filter((id) => id !== rowId));
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
      packages: packageRows.map((rowId) => ({
        description: text(`package-${rowId}-description`),
        weightKg: Number(form.get(`package-${rowId}-weightKg`)),
        dimensions: text(`package-${rowId}-dimensions`),
        quantity: Number(form.get(`package-${rowId}-quantity`)),
        declaredValue: Number(form.get(`package-${rowId}-declaredValue`)),
        fragile: form.get(`package-${rowId}-fragile`) === "on",
      })),
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
      setPackageRows([nextPackageId.current++]);
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

  async function openShipmentDetails(shipmentId: number) {
    if (!session) return;
    if (expandedShipmentId === shipmentId) {
      setExpandedShipmentId(null);
      return;
    }

    setExpandedShipmentId(shipmentId);
    if (Object.prototype.hasOwnProperty.call(routes, shipmentId)) return;

    setBusyAction(`route-load-${shipmentId}`);
    try {
      const route = await apiRequest<DeliveryRoute>(`/routes/${shipmentId}`, {}, session.token);
      setRoutes((current) => ({ ...current, [shipmentId]: route }));
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setRoutes((current) => ({ ...current, [shipmentId]: null }));
      } else {
        setRoutes((current) => ({ ...current, [shipmentId]: null }));
        handleApiFailure(error, "Unable to load route details.");
      }
    } finally {
      setBusyAction(null);
    }
  }

  async function assignOperator(event: FormEvent<HTMLFormElement>, shipment: Shipment) {
    event.preventDefault();
    if (!session) return;
    const form = new FormData(event.currentTarget);
    const operatorId = Number(form.get("operatorId"));
    if (!operatorId) {
      setMessage("Select an active logistics operator first.");
      return;
    }

    setMessage("");
    setSuccess("");
    setBusyAction(`operator-${shipment.id}`);
    try {
      const updated = await apiRequest<Shipment>(`/shipments/${shipment.id}/operator`, {
        method: "PATCH",
        body: JSON.stringify({ operatorId }),
      }, session.token);
      setShipments((current) => current.map((item) => item.id === updated.id ? updated : item));
      setSuccess(`${updated.trackingNumber} assigned to ${updated.assignedOperator}.`);
    } catch (error) {
      handleApiFailure(error, "Unable to assign the operator.");
    } finally {
      setBusyAction(null);
    }
  }

  async function createRoute(event: FormEvent<HTMLFormElement>, shipment: Shipment) {
    event.preventDefault();
    if (!session) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const text = (name: string) => String(form.get(name) ?? "").trim();
    const payload: RouteRequest = {
      shipmentId: shipment.id,
      driverName: text("driverName") || null,
      driverPhone: text("driverPhone") || null,
      vehicleNumber: text("vehicleNumber") || null,
    };

    setMessage("");
    setSuccess("");
    setBusyAction(`route-create-${shipment.id}`);
    try {
      const route = await apiRequest<DeliveryRoute>("/routes", {
        method: "POST",
        body: JSON.stringify(payload),
      }, session.token);
      setRoutes((current) => ({ ...current, [shipment.id]: route }));
      setSuccess(`Route created for ${shipment.trackingNumber}.`);
    } catch (error) {
      handleApiFailure(error, "Unable to create the route.");
    } finally {
      setBusyAction(null);
    }
  }

  async function updateDriver(event: FormEvent<HTMLFormElement>, route: DeliveryRoute) {
    event.preventDefault();
    if (!session) return;
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? "").trim();
    const payload: DriverAssignmentRequest = {
      driverName: text("driverName"),
      driverPhone: text("driverPhone") || null,
      vehicleNumber: text("vehicleNumber") || null,
    };

    setMessage("");
    setSuccess("");
    setBusyAction(`driver-${route.id}`);
    try {
      const updated = await apiRequest<DeliveryRoute>(`/routes/${route.id}/driver`, {
        method: "PATCH",
        body: JSON.stringify(payload),
      }, session.token);
      setRoutes((current) => ({ ...current, [updated.shipmentId]: updated }));
      setSuccess(`Driver updated for ${updated.trackingNumber}.`);
    } catch (error) {
      handleApiFailure(error, "Unable to update the driver.");
    } finally {
      setBusyAction(null);
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
          {canCreateShipments && (
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

        {!canViewShipments && (
          <section className="empty-card">
            <span className="empty-icon">i</span>
            <h2>Your account is active</h2>
            <p>This role does not have access to the shipment workspace.</p>
          </section>
        )}

        {showCreate && canCreateShipments && (
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
                <legend>Packages</legend>
                <div className="package-settings">
                  <div className="field"><label htmlFor="priority">Shipment priority</label><select id="priority" name="priority" defaultValue="STANDARD"><option value="STANDARD">Standard</option><option value="EXPRESS">Express</option></select></div>
                  <button className="secondary-button package-add-button" type="button" onClick={addPackageRow}><Plus size={15} /> Add package</button>
                </div>
                <div className="package-list">
                  {packageRows.map((rowId, index) => (
                    <section className="package-entry" key={rowId} aria-label={`Package ${index + 1}`}>
                      <div className="package-entry-heading">
                        <strong>Package {index + 1}</strong>
                        {packageRows.length > 1 && (
                          <button className="package-remove-button" type="button" onClick={() => removePackageRow(rowId)} aria-label={`Remove package ${index + 1}`}><Trash2 size={14} /> Remove</button>
                        )}
                      </div>
                      <div className="form-grid four-columns">
                        <div className="field span-two"><label htmlFor={`package-${rowId}-description`}>Description</label><input id={`package-${rowId}-description`} name={`package-${rowId}-description`} maxLength={500} required /></div>
                        <div className="field"><label htmlFor={`package-${rowId}-weightKg`}>Weight (kg)</label><input id={`package-${rowId}-weightKg`} name={`package-${rowId}-weightKg`} type="number" min="0.01" max="99999999.99" step="0.01" required /></div>
                        <div className="field"><label htmlFor={`package-${rowId}-quantity`}>Quantity</label><input id={`package-${rowId}-quantity`} name={`package-${rowId}-quantity`} type="number" min="1" step="1" defaultValue="1" required /></div>
                        <div className="field span-two"><label htmlFor={`package-${rowId}-dimensions`}>Dimensions</label><input id={`package-${rowId}-dimensions`} name={`package-${rowId}-dimensions`} placeholder="30 × 20 × 15 cm" maxLength={100} required /></div>
                        <div className="field"><label htmlFor={`package-${rowId}-declaredValue`}>Declared value (₹)</label><input id={`package-${rowId}-declaredValue`} name={`package-${rowId}-declaredValue`} type="number" min="0" step="0.01" required /></div>
                        <label className="check-card"><input name={`package-${rowId}-fragile`} type="checkbox" /><span><strong>Fragile</strong><small>Handle with care</small></span></label>
                      </div>
                    </section>
                  ))}
                </div>
              </fieldset>
              <div className="form-actions">
                <button className="secondary-button" type="button" onClick={() => setShowCreate(false)}>Cancel</button>
                <button className="primary-button action-button" type="submit" disabled={saving}>{saving ? "Creating..." : <><Plus size={17} /> Create shipment</>}</button>
              </div>
            </form>
          </section>
        )}

        {canViewShipments && (
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
                  const transitions = canUpdateShipmentStatus ? ALLOWED_TRANSITIONS[shipment.status] : [];
                  const canCancel = session.user.role === "ADMINISTRATOR"
                    || (["CUSTOMER", "BUSINESS_CLIENT"].includes(session.user.role) && shipment.createdById === session.user.id)
                    || (session.user.role === "LOGISTICS_OPERATOR" && shipment.assignedOperatorId === session.user.id);
                  const isExpanded = expandedShipmentId === shipment.id;
                  const route = routes[shipment.id];
                  const routeLoading = busyAction === `route-load-${shipment.id}`;
                  const canCreateThisRoute = session.user.role === "ADMINISTRATOR"
                    || (session.user.role === "LOGISTICS_OPERATOR" && shipment.assignedOperatorId === session.user.id);
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
                          <div><small>Packages</small><strong>{shipment.packages.length}</strong></div>
                        </div>
                      </div>
                      <div className="shipment-actions">
                        {transitions.length > 0 && (
                          <div>
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
                          </div>
                        )}
                        {canCancel && shipment.status !== "CANCELLED" && shipment.status !== "DELIVERED" && (
                          <button className="danger-button" type="button" onClick={() => void cancelShipment(shipment)}>Cancel shipment</button>
                        )}
                        {transitions.length === 0 && (!canCancel || shipment.status === "CANCELLED" || shipment.status === "DELIVERED") && (
                          <span className="terminal-state">No further status actions</span>
                        )}
                        <button
                          className={`secondary-button details-button${isExpanded ? " active" : ""}`}
                          type="button"
                          aria-expanded={isExpanded}
                          onClick={() => void openShipmentDetails(shipment.id)}
                        >
                          {canManageRoutes ? <RouteIcon size={14} /> : <MapPinned size={14} />}
                          {canManageRoutes ? "Manage delivery" : "View details"}
                          <ChevronDown className="details-chevron" size={14} />
                        </button>
                      </div>

                      {isExpanded && (
                        <section className="shipment-detail-panel">
                          <div className="detail-panel-heading">
                            <div>
                              <span className="eyebrow dark">Shipment workspace</span>
                              <h3>Package, assignment and route</h3>
                            </div>
                            <span className="detail-reference">#{shipment.id}</span>
                          </div>

                          <div className="operations-grid">
                            <article className="operation-card package-operation">
                              <div className="operation-title">
                                <span className="operation-icon"><Package size={16} /></span>
                                <div><strong>Package manifest</strong><small>{shipment.packages.length} package{shipment.packages.length === 1 ? "" : "s"}</small></div>
                              </div>
                              {shipment.packages.length === 0 ? (
                                <p className="operation-empty">No package records are linked to this older shipment.</p>
                              ) : (
                                <div className="manifest-list">
                                  {shipment.packages.map((shipmentPackage, index) => (
                                    <div className="manifest-item" key={shipmentPackage.id}>
                                      <span>{String(index + 1).padStart(2, "0")}</span>
                                      <div><strong>{shipmentPackage.description}</strong><small>{shipmentPackage.quantity} × {shipmentPackage.weightKg} kg · {shipmentPackage.dimensions}</small></div>
                                      <div className="manifest-value"><strong>{formatCurrency(shipmentPackage.declaredValue)}</strong><small>{shipmentPackage.fragile ? "Fragile" : "Standard handling"}</small></div>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </article>

                            {session.user.role === "ADMINISTRATOR" && (
                              <article className="operation-card assignment-operation">
                                <div className="operation-title">
                                  <span className="operation-icon"><UserRoundCheck size={16} /></span>
                                  <div><strong>Operator assignment</strong><small>Controls operator visibility</small></div>
                                </div>
                                <div className="current-assignment">
                                  <small>Currently assigned</small>
                                  <strong>{shipment.assignedOperator ?? "No operator assigned"}</strong>
                                </div>
                                <form onSubmit={(event) => void assignOperator(event, shipment)}>
                                  <div className="field">
                                    <label htmlFor={`operator-${shipment.id}`}>Logistics operator</label>
                                    <select id={`operator-${shipment.id}`} name="operatorId" defaultValue={shipment.assignedOperatorId ?? ""} required>
                                      <option value="" disabled>Select an active operator</option>
                                      {operators.map((operator) => <option value={operator.id} key={operator.id}>{operator.fullName} · {operator.email}</option>)}
                                    </select>
                                  </div>
                                  {operators.length === 0 && <p className="inline-note">Register an active Logistics Operator to enable assignment.</p>}
                                  <button className="secondary-button operation-submit" type="submit" disabled={operators.length === 0 || busyAction === `operator-${shipment.id}`}>
                                    <UserRoundCheck size={14} />
                                    {busyAction === `operator-${shipment.id}` ? "Assigning..." : shipment.assignedOperatorId ? "Change operator" : "Assign operator"}
                                  </button>
                                </form>
                              </article>
                            )}

                            <article className="operation-card route-operation">
                              <div className="operation-title">
                                <span className="operation-icon"><Navigation size={16} /></span>
                                <div><strong>Route management</strong><small>Directions and driver details</small></div>
                              </div>

                              {routeLoading || route === undefined ? (
                                <div className="operation-loading"><span className="spinner dark-spinner" />Calculating workspace...</div>
                              ) : route === null ? (
                                <div className="route-create-state">
                                  <div className="route-summary compact-route">
                                    <div><small>Origin</small><strong>{shipment.pickupAddress}</strong></div>
                                    <span className="route-summary-line"><Truck size={15} /></span>
                                    <div><small>Destination</small><strong>{shipment.deliveryAddress}</strong></div>
                                  </div>
                                  {canCreateThisRoute ? (
                                    <form className="driver-form" onSubmit={(event) => void createRoute(event, shipment)}>
                                      <p className="inline-note">Google Maps will calculate coordinates, distance and travel time automatically. The route is still saved if Maps is unavailable.</p>
                                      <div className="form-grid three-columns">
                                        <div className="field"><label htmlFor={`new-driver-${shipment.id}`}>Driver name <span className="optional">optional</span></label><input id={`new-driver-${shipment.id}`} name="driverName" maxLength={120} /></div>
                                        <div className="field"><label htmlFor={`new-driver-phone-${shipment.id}`}>Driver phone <span className="optional">optional</span></label><input id={`new-driver-phone-${shipment.id}`} name="driverPhone" maxLength={25} /></div>
                                        <div className="field"><label htmlFor={`new-vehicle-${shipment.id}`}>Vehicle number <span className="optional">optional</span></label><input id={`new-vehicle-${shipment.id}`} name="vehicleNumber" maxLength={40} /></div>
                                      </div>
                                      <button className="primary-button route-submit" type="submit" disabled={busyAction === `route-create-${shipment.id}`}>
                                        <RouteIcon size={15} />{busyAction === `route-create-${shipment.id}` ? "Creating route..." : "Create route"}
                                      </button>
                                    </form>
                                  ) : (
                                    <p className="operation-empty">A logistics operator has not created a route for this shipment yet.</p>
                                  )}
                                </div>
                              ) : (
                                <div className="route-record">
                                  <div className="route-summary">
                                    <div><small>Origin</small><strong>{route.originAddress}</strong></div>
                                    <span className="route-summary-line"><Truck size={15} /></span>
                                    <div><small>Destination</small><strong>{route.destinationAddress}</strong></div>
                                  </div>
                                  <div className="route-metrics">
                                    <div><small>Distance</small><strong>{route.distanceKm === null ? "Pending" : `${Number(route.distanceKm).toFixed(2)} km`}</strong></div>
                                    <div><small>Estimated travel</small><strong>{formatDuration(route.estimatedTimeMinutes)}</strong></div>
                                    <div><small>Vehicle</small><strong>{route.vehicleNumber ?? "Not assigned"}</strong></div>
                                    <div><small>Driver</small><strong>{route.driverName ?? "Not assigned"}</strong></div>
                                  </div>
                                  <div className="coordinate-strip">
                                    <span>Origin {formatCoordinate(route.originLatitude)}, {formatCoordinate(route.originLongitude)}</span>
                                    <span>Destination {formatCoordinate(route.destinationLatitude)}, {formatCoordinate(route.destinationLongitude)}</span>
                                  </div>
                                  {route.distanceKm === null && (
                                    <p className="map-pending-note">Route saved successfully. Distance and travel time will appear after Google Maps responds.</p>
                                  )}
                                  {canManageRoutes && (
                                    <form className="driver-form existing-driver-form" onSubmit={(event) => void updateDriver(event, route)}>
                                      <span className="form-kicker">Assign or change driver</span>
                                      <div className="form-grid three-columns">
                                        <div className="field"><label htmlFor={`driver-${route.id}`}>Driver name</label><input id={`driver-${route.id}`} name="driverName" defaultValue={route.driverName ?? ""} maxLength={120} required /></div>
                                        <div className="field"><label htmlFor={`driver-phone-${route.id}`}>Driver phone</label><input id={`driver-phone-${route.id}`} name="driverPhone" defaultValue={route.driverPhone ?? ""} maxLength={25} /></div>
                                        <div className="field"><label htmlFor={`vehicle-${route.id}`}>Vehicle number</label><input id={`vehicle-${route.id}`} name="vehicleNumber" defaultValue={route.vehicleNumber ?? ""} maxLength={40} /></div>
                                      </div>
                                      <button className="secondary-button operation-submit" type="submit" disabled={busyAction === `driver-${route.id}`}>
                                        <Truck size={14} />{busyAction === `driver-${route.id}` ? "Updating..." : route.driverName ? "Update driver" : "Assign driver"}
                                      </button>
                                    </form>
                                  )}
                                </div>
                              )}
                            </article>
                          </div>
                        </section>
                      )}
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
