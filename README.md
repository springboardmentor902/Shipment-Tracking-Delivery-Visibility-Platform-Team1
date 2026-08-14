<div align="center">
  <img src="frontend/public/brand/shiptrack-mark.png" alt="ShipTrack Pro logo" width="76" />
  <h1>ShipTrack Pro</h1>
  <p>A simple full-stack platform for creating shipments and tracking their delivery status.</p>
</div>

## What is included?

- Registration and login with JWT authentication
- Role-based access control for five user roles
- Shipment creation, listing, detail, status update and cancellation
- A closed shipment lifecycle with validated status transitions
- PostgreSQL as the runtime database
- A separate Next.js frontend with a responsive dark interface
- Backend unit and integration tests

Shipment management is available only to `BUSINESS_CLIENT` and `LOGISTICS_OPERATOR` users.

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
export SPRING_DATASOURCE_PASSWORD='your-postgresql-password'
export JWT_SECRET_KEY='replace-this-with-a-random-secret-of-at-least-64-characters'
DEBUG=false ./mvnw spring-boot:run
```

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
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in the browser.

The frontend sends `/api/*` requests to the Spring Boot backend. To use another backend URL, copy `frontend/.env.example` to `frontend/.env.local` and change `BACKEND_URL`.

## How to verify the project

The easiest check is through the web interface:

1. Open the registration page.
2. Create an account with the `Business client` or `Logistics operator` role.
3. Sign in using that account.
4. Create a shipment from the dashboard.
5. Confirm that it starts in `CREATED` status and receives a tracking number.
6. Move it through `PICKED_UP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY` and `DELIVERED`.
7. Create another shipment and cancel it with a reason.

Cancellation keeps the database record and changes its status to `CANCELLED`; it does not physically delete the shipment.

### Development administrator

The backend seeds one administrator account:

```text
Email:    admin@shiptrack.com
Password: Admin@123
```

The administrator manages users and roles. Use a business client or logistics operator account for the shipment demo.

## Shipment API

Every shipment request needs a valid JWT from a `BUSINESS_CLIENT` or `LOGISTICS_OPERATOR`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/shipments` | Create a shipment |
| `GET` | `/api/shipments` | Fetch all shipments |
| `GET` | `/api/shipments/{id}` | Fetch one shipment |
| `PATCH` or `PUT` | `/api/shipments/{id}/status` | Update status or location |
| `DELETE` | `/api/shipments/{id}?reason=...` | Cancel a shipment |

### Shipment lifecycle

```text
CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
                            │                │
                            └─ FAILED_DELIVERY ─┘

Non-terminal states can also move to CANCELLED.
```

`DELIVERED` and `CANCELLED` are terminal states. Invalid status jumps return `409 Conflict`.

## Roles

| Role | Current access |
| --- | --- |
| `CUSTOMER` | Registration and login |
| `BUSINESS_CLIENT` | Full shipment management |
| `LOGISTICS_OPERATOR` | Full shipment management |
| `SUPPORT_AGENT` | Registration and login |
| `ADMINISTRATOR` | User and role administration |

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
