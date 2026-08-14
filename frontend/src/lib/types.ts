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
  packageDescription: string;
  weightKg: number;
  dimensions: string;
  quantity: number;
  declaredValue: number;
  fragile: boolean;
  status: ShipmentStatus;
  currentLocation: string | null;
  estimatedDeliveryDate: string;
  actualDeliveryDate: string | null;
  cancellationReason: string | null;
  createdById: number;
  createdBy: string;
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
  packageDescription: string;
  weightKg: number;
  dimensions: string;
  quantity: number;
  declaredValue: number;
  fragile: boolean;
}
