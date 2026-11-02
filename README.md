# P2P Payment Ledger

## Overview

P2P Payment Ledger is a wallet and peer-to-peer transfer application. Users can
manage their wallet and send money to other registered users. Administrators
can monitor activity, review alerts, and manage wallets and settlements.

The backend is a Java 21 / Spring Boot application backed by PostgreSQL. The
frontend is a React application built with Vite.

## Implemented features

- Account registration and login with role-based access.
- Wallet balance, deposits, and withdrawals.
- Transfers to users found by email or phone number, protected by a transaction
  PIN.
- Pending transfers with scheduled settlement, status updates, and refunds for
  failed or declined settlements.
- Paginated transaction history with date, sent/received, and status filters.
- Notifications for account and transaction activity.
- Duplicate-transfer detection, a daily outgoing limit, and high-value transfer
  alerts.
- Admin dashboard with today's settled debit/credit reconciliation and a
  separate pending settlement total.
- Admin user search by email or phone number, wallet freeze/unfreeze, transaction
  and settlement monitoring, fraud-alert actions, and recent settlement jobs.
- Audit records for administrative actions.

## How to run

### Requirements

- Java 21
- Node.js and npm
- PostgreSQL

### 1. Prepare PostgreSQL

Create a PostgreSQL database for the application. The default database name is
`ledger`:

```sql
CREATE DATABASE ledger;
```

Configure the backend connection and JWT signing key using environment
variables. For local development, set:

- `SPRING_DATASOURCE_URL` — JDBC URL, for example
  `jdbc:postgresql://localhost:5432/ledger`
- `SPRING_DATASOURCE_USERNAME` — PostgreSQL username
- `SPRING_DATASOURCE_PASSWORD` — PostgreSQL password
- `SECRET_KEY` — a stable, randomly generated signing key of at least 32 bytes

Keep the JWT signing key stable between restarts so existing tokens remain
verifiable until they expire. Do not commit credentials or production secrets.

### 2. Start the backend

From the repository root, set the environment variables above and run the
Spring Boot application:

```sh
./mvnw spring-boot:run
```

On Windows PowerShell, use the Maven wrapper script:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8080` by default. Hibernate is
configured to update the database schema on startup.

### 3. Start the frontend

In another terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open the local URL printed by Vite (by default, `http://localhost:5173`). The
development server proxies API requests to `http://localhost:8080`. To use a
different backend address, set `VITE_API_PROXY_TARGET` in the frontend
environment.

Public registration creates a standard user account. The admin area requires
an account provisioned with the `ADMIN` role.

### Build and test

Build the frontend from the `frontend` directory:

```sh
npm run build
```

Run backend tests from the repository root:

```sh
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```
