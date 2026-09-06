<div align="center">
  <img src="frontend/public/brand/shiptrack-mark.png" alt="ShipTrack Pro logo" width="76" />
  <h1>ShipTrack Pro</h1>
  <p>A simple full-stack platform for creating shipments and tracking their delivery status.</p>
</div>

## What is included?

- Registration and login with JWT authentication
- Role-based access control for five user roles
- Shipment creation with multiple packages, listing, detail, status update and cancellation
- User-specific shipment lists: customers see their own, operators see assigned and administrators see all
- Route creation, driver assignment and route lookup by shipment
- Live driver location updates with authenticated STOMP over WebSocket
- Google Maps with Geoapify fallback for geocoding, distance and travel time
- Rules-based ETA prediction with delay-risk and confidence scores
- Automatic ETA recalculation after tracking updates and every 20 minutes
- In-app and email notifications for shipment updates and delay warnings
- Proof of Delivery with signature, delivery photo and Support/Admin verification
- A closed shipment lifecycle with validated status transitions
- PostgreSQL as the runtime database
- A separate Next.js frontend with a responsive dark interface
- Backend unit and integration tests

Shipment data is filtered by the authenticated user's role and relationship to the shipment.

## Project structure

```text
Shipment-Tracking-Delivery-Visibility-Platform-Team1/
├── shiptrack-pro/   Spring Boot REST API (port 8080)
├── frontend/        Next.js application (port 3000)
└── README.md
```

The frontend and backend are intentionally separate. Spring Boot only serves JSON APIs; Next.js handles the web interface.

## Requirements

Install these before starting:

- Java 17 or newer
- PostgreSQL
- Node.js 20.9 or newer
- npm

You can confirm the installations with:

```bash
java --version
psql --version
node --version
npm --version
```

## First-time database setup

The application expects a PostgreSQL database named `shiptrack`.

```bash
sudo -u postgres psql
```

Inside PostgreSQL, run:

```sql
CREATE DATABASE shiptrack;
ALTER USER postgres WITH PASSWORD 'your-postgresql-password';
\q
```

If the database already exists, skip the `CREATE DATABASE` command.

## Start the application

Use two terminals: one for the backend and one for the frontend.

### 1. Start the backend

```bash
cd shiptrack-pro
test -f .env || cp .env.example .env
# Fill the local .env once, then run:
set -a
source .env
set +a
DEBUG=false ./mvnw spring-boot:run
```

Google Maps is used first when `GOOGLE_MAPS_API_KEY` is configured. Otherwise Geoapify uses `GEOAPIFY_GEOCODING_API_KEY` and `GEOAPIFY_ROUTING_API_KEY`. If every provider fails, the route is still saved without map metrics. The real `.env` file is ignored by Git.

Email delivery is optional for local development. Add `MAIL_USERNAME`, `MAIL_PASSWORD` and `MAIL_FROM` to `.env`, then set `MAIL_ENABLED=true`. Without SMTP credentials, in-app notifications are still created and visible from the bell icon.

The backend is ready when the terminal shows:

```text
Started ShiptrackProApplication
```

API base URL: `http://localhost:8080/api`

### 2. Start the frontend

Open another terminal:

```bash
cd frontend
npm install
test -f .env.local || cp .env.example .env.local
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in the browser.

The frontend sends `/api/*` requests to the Spring Boot backend. Add either a browser-restricted Google key or `NEXT_PUBLIC_GEOAPIFY_API_KEY` to `.env.local`. Geoapify uses a dark MapLibre map with visible provider attribution.

## How to verify the project

The easiest check is through the web interface:

1. Open the registration page.
2. Create a `Customer`, `Business client` or `Logistics operator` account.
3. Sign in using that account.
4. Create a shipment with one or more packages from the dashboard.
5. Confirm that it starts in `CREATED` status and receives a tracking number.
6. Move it through `PICKED_UP`, `IN_TRANSIT` and `OUT_FOR_DELIVERY`.
7. Sign in as the assigned Logistics Operator and select `Complete delivery`. Add the recipient signature, delivery photo, name and notes.
8. Confirm that the shipment becomes `DELIVERED`, then open its full detail page as the Customer to view the proof.
9. Sign in as the administrator, open the proof and verify or reject it.
10. Create another shipment and cancel it with a reason.
11. Open `Manage delivery`, create a route, then select `Open live tracking`.
12. In an Operator/Admin session, broadcast coordinates and confirm that an open Customer tracking page moves immediately.
13. Open `Full detail` to show the predicted delivery time, delay risk, confidence and calculation factors.
14. For the Business Client risk demo, use `SEVERE` traffic and move a shipment to `FAILED_DELIVERY`; predictions above 6 appear in `At Risk`.
15. Open the notification bell after a tracking update, then select the new notification to mark it as read.

Cancellation keeps the database record and changes its status to `CANCELLED`; it does not physically delete the shipment.

### Development administrator

The backend seeds one administrator account:

```text
Email:    admin@shiptrack.com
Password: Admin@123
```

The administrator manages users and roles. Use a customer or business client account to demonstrate owner-only shipment visibility. Use a logistics operator account for status and route management.

## Shipment API

Every shipment request needs a valid JWT. The returned data and available actions depend on the authenticated role.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/shipments` | Create a shipment |
| `GET` | `/api/shipments` | Fetch own, assigned or all shipments according to role |
| `GET` | `/api/shipments/{id}` | Fetch one shipment when the user has access |
| `PATCH` or `PUT` | `/api/shipments/{id}/status` | Update status or location |
| `PATCH` | `/api/shipments/{id}/operator` | Assign an operator (administrator only) |
| `DELETE` | `/api/shipments/{id}?reason=...` | Cancel a shipment |

The create request contains a `packages` array. Each package is stored in the separate `packages` table with the shipment id as its foreign key.

## Route API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/routes` | Create a route for a shipment (operator/admin) |
| `GET` | `/api/routes/{shipmentId}` | Fetch the accessible shipment's route |
| `PATCH` | `/api/routes/{routeId}/driver` | Assign or change route driver (operator/admin) |
| `POST` | `/api/route/{routeId}/location` | Save and broadcast the driver's latest coordinates (assigned operator/admin) |

Route origin and destination come from the shipment's pickup and delivery addresses. Google Maps is the primary provider and Geoapify is the automatic fallback. A provider error does not stop route creation.

`trafficCondition` accepts `UNKNOWN`, `LIGHT`, `MODERATE`, `HEAVY` or `SEVERE`. It is used by ETA prediction.

## ETA API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/eta/{shipmentId}/predict` | Recalculate ETA (assigned operator/admin) |
| `GET` | `/api/eta/{shipmentId}` | Fetch the current prediction when the shipment is visible |

Each prediction is stored in `eta_predictions` with the predicted delivery time, risk score from 0 to 10, confidence from 0 to 100, readable factors and calculation time. Status and location changes are saved in `tracking_events` and trigger recalculation after the database transaction commits. A scheduled job also refreshes every in-progress shipment every 20 minutes. The at-risk threshold defaults to 6 and can be changed with `ETA_AT_RISK_THRESHOLD`.

## Notification API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/notifications` | Fetch the logged-in user's notifications, newest first |
| `PATCH` | `/api/notifications/{id}/read` | Mark one owned notification as read |
| `POST` | `/api/notification` | Create a notification through the service (administrator only) |

A new tracking event creates a `SHIPMENT_UPDATE` notification for the shipment owner. When ETA delay risk crosses the configured threshold, it creates a `DELAY_WARNING`. The owner receives the in-app notification and email; the shipment receiver also receives a separate email. If both addresses match, only one email is sent. The service suppresses the same notification type for the same user and shipment for five minutes by default; this can be changed with `NOTIFICATION_DUPLICATE_WINDOW_MINUTES`.

## Proof of Delivery API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/pod/{shipmentId}` | Assigned Logistics Operator submits recipient name, notes, signature and photo |
| `GET` | `/api/pod/{shipmentId}` | View proof when the shipment is accessible |
| `PATCH` | `/api/pod/{shipmentId}/verify` | Support Agent/Admin verifies or rejects the proof |

Submission uses `multipart/form-data` fields named `recipientName`, `deliveryNotes`, `signature` and `photo`. The shipment must be `OUT_FOR_DELIVERY`; a successful submission changes it to `DELIVERED` and records the actual delivery time. PNG, JPEG and WebP images up to 5 MB are stored under the ignored `POD_UPLOAD_DIR`. Stored images are served only after JWT and shipment-access checks.

## Live tracking

- STOMP handshake endpoint: `/api/ws/tracking`
- Per-shipment destination: `/topic/shipments/{shipmentId}/location`
- The STOMP `CONNECT` frame must include `Authorization: Bearer <jwt>`.
- Subscription access follows the same rule as shipment visibility: owner Customer/Business Client, assigned Operator, or Admin.
- Location is persisted in the route's `last_known_latitude`, `last_known_longitude`, and `last_location_updated_at` columns before it is broadcast.
- The tracking page unsubscribes and deactivates its STOMP client when the user navigates away.

Backend keys remain server-side. The browser map uses either `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY` or `NEXT_PUBLIC_GEOAPIFY_API_KEY` from `frontend/.env.local`.

### Shipment lifecycle

```text
CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
                            │                │
                            └─ FAILED_DELIVERY ─┘

Non-terminal states can also move to CANCELLED.
```

`DELIVERED` and `CANCELLED` are terminal states. Invalid status jumps return `409 Conflict`.
The `OUT_FOR_DELIVERY → DELIVERED` transition is completed by submitting Proof of Delivery, not by the generic status endpoint.

## Roles

| Role | Current access |
| --- | --- |
| `CUSTOMER` | Create shipments and view/cancel own shipments and routes |
| `BUSINESS_CLIENT` | Customer shipment functions for its own records |
| `LOGISTICS_OPERATOR` | View assigned shipments, manage status/routes/drivers and submit Proof of Delivery |
| `SUPPORT_AGENT` | Verify or reject Proof of Delivery records |
| `ADMINISTRATOR` | All shipments/routes, user/role/operator assignment and Proof of Delivery verification |

Public registration cannot create an administrator. Only the startup seeder creates the single administrator account.

## Run checks before committing

### Backend

```bash
cd shiptrack-pro
DEBUG=false ./mvnw clean test
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

Backend tests use an isolated in-memory database. The running application uses PostgreSQL.
