export type UserRole =
  | "BUSINESS_CLIENT"
  | "LOGISTICS_OPERATOR"
  | "CUSTOMER"
  | "SUPPORT_AGENT"
  | "ADMINISTRATOR";

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: UserRole;
  status: string;
  createdAt: string;
}

export interface AuthSession {
  token: string;
  tokenType: string;
  user: User;
}

export const SHIPMENT_STATUSES = [
  "CREATED",
  "PICKED_UP",
  "IN_TRANSIT",
  "OUT_FOR_DELIVERY",
  "DELIVERED",
  "FAILED_DELIVERY",
  "CANCELLED",
] as const;

export type ShipmentStatus = (typeof SHIPMENT_STATUSES)[number];
export type ShipmentPriority = "STANDARD" | "EXPRESS";
export type TrafficCondition = "UNKNOWN" | "LIGHT" | "MODERATE" | "HEAVY" | "SEVERE";

export interface ShipmentPackage {
  id: number;
  description: string;
  weightKg: number;
  dimensions: string;
  quantity: number;
  declaredValue: number;
  fragile: boolean;
}

export type ShipmentPackageRequest = Omit<ShipmentPackage, "id">;

export interface Shipment {
  id: number;
  trackingNumber: string;
  senderName: string;
  senderPhone: string | null;
  senderAddress: string;
  receiverName: string;
  receiverPhone: string | null;
  receiverEmail: string;
  receiverAddress: string;
  pickupAddress: string;
  deliveryAddress: string;
  priority: ShipmentPriority;
  packages: ShipmentPackage[];
  status: ShipmentStatus;
  currentLocation: string | null;
  estimatedDeliveryDate: string;
  actualDeliveryDate: string | null;
  cancellationReason: string | null;
  createdById: number;
  createdBy: string;
  assignedOperatorId: number | null;
  assignedOperator: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ShipmentRequest {
  senderName: string;
  senderPhone: string | null;
  senderAddress: string;
  receiverName: string;
  receiverPhone: string | null;
  receiverEmail: string;
  receiverAddress: string;
  pickupAddress: string;
  deliveryAddress: string;
  priority: ShipmentPriority;
  packages: ShipmentPackageRequest[];
}

export interface DeliveryRoute {
  id: number;
  shipmentId: number;
  trackingNumber: string;
  originAddress: string;
  destinationAddress: string;
  originLatitude: number | null;
  originLongitude: number | null;
  destinationLatitude: number | null;
  destinationLongitude: number | null;
  distanceKm: number | null;
  estimatedTimeMinutes: number | null;
  trafficCondition: TrafficCondition;
  lastKnownLatitude: number | null;
  lastKnownLongitude: number | null;
  lastLocationUpdatedAt: string | null;
  driverName: string | null;
  driverPhone: string | null;
  vehicleNumber: string | null;
  createdById: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface RouteRequest {
  shipmentId: number;
  trafficCondition: TrafficCondition;
  driverName: string | null;
  driverPhone: string | null;
  vehicleNumber: string | null;
}

export interface DriverAssignmentRequest {
  driverName: string;
  driverPhone: string | null;
  vehicleNumber: string | null;
}

export interface LocationUpdate {
  routeId: number;
  shipmentId: number;
  trackingNumber: string;
  latitude: number;
  longitude: number;
  recordedAt: string;
}

export interface ETAPrediction {
  id: number;
  shipmentId: number;
  trackingNumber: string;
  predictedDeliveryTime: string;
  delayRiskScore: number;
  confidenceScore: number;
  factors: string;
  calculatedAt: string;
  atRisk: boolean;
}

export type NotificationType = "SHIPMENT_UPDATE" | "DELAY_WARNING";
export type NotificationStatus = "PENDING" | "SENT" | "FAILED";

export interface NotificationRecord {
  id: number;
  userId: number;
  shipmentId: number;
  trackingNumber: string;
  type: NotificationType;
  title: string;
  message: string;
  status: NotificationStatus;
  sentAt: string | null;
  readAt: string | null;
  createdAt: string;
  unread: boolean;
}

export type ProofVerificationStatus = "PENDING" | "VERIFIED" | "REJECTED";

export interface ProofOfDelivery {
  id: number;
  shipmentId: number;
  trackingNumber: string;
  recipientName: string;
  deliveryNotes: string | null;
  signatureUrl: string;
  photoUrl: string;
  submittedById: number;
  submittedBy: string;
  submittedAt: string;
  verificationStatus: ProofVerificationStatus;
  verifiedById: number | null;
  verifiedBy: string | null;
  verifiedAt: string | null;
  verificationNotes: string | null;
}
