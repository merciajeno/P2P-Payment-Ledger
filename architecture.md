# P2P Payment Ledger — Architecture

A simple overview of the main parts of the application and how they work
together.

## 1. Application structure

```mermaid
flowchart LR
    Person["User or admin"] --> Frontend["React website"]
    Frontend --> Security["Login and access checks"]
    Security --> API["Backend API"]
    API --> Services["Application logic"]
    Services --> Database[("PostgreSQL database")]
```

The website sends requests to the backend. The backend checks access, applies
the application rules, and reads or saves data in the database.

## 2. Registration and login

```mermaid
flowchart LR
    User["User"] --> Website["Registration or login form"]
    Website --> Backend["Authentication service"]
    Backend --> Database[("Users and login attempts")]
    Backend -->|Successful login| Session["Signed-in session"]
    Session --> Website
```

Registration creates the user and their wallet. At login, the backend checks
the password and tracks failed attempts. A successful login starts an
authenticated session.

## 3. Wallet

```mermaid
flowchart LR
    User["User"] --> Website["Wallet page"]
    Website --> API["Wallet and transaction API"]
    API --> Logic["Check wallet and amount"]
    Logic --> Database[("Wallet balance and ledger")]
```

Users can view their balance and deposit or withdraw simulated funds. Wallet
status determines whether money operations are allowed.

## 4. Sending and settling a transfer

```mermaid
flowchart LR
    Sender["Sender"] --> Form["Choose recipient, amount,<br/>remark, and PIN"]
    Form --> Checks["Validate request and<br/>check transfer rules"]
    Checks -->|Allowed| Pending["Debit sender<br/>Record pending transfer"]
    Pending --> Job["Settlement job"]
    Job -->|Successful settlement| Credit["Credit receiver<br/>Record ledger entry"]
    Job -->|Settlement declined or fails| Refund["Refund sender"]
    Credit --> Done["Notify both users"]
    Refund --> Done
```

The sender's money is held as a pending transfer until settlement. The receiver
is credited after settlement; if settlement cannot complete, the sender is
refunded.

## 5. Fraud checks

```mermaid
flowchart LR
    Transfer["Transfer request"] --> Duplicate["Check for recent duplicate"]
    Duplicate --> Limit["Check daily outgoing limit"]
    Limit --> Value["Check high-value threshold"]
    Value --> Outcome["Allow transfer<br/>or create fraud alert"]
    Outcome --> Admin["Admin reviews alert"]
```

The backend checks transfers before processing them. A high-value transfer can
proceed while being flagged for admin review; blocked checks reject the
transfer.

## 6. Notifications

```mermaid
flowchart LR
    Event["Transfer, account,<br/>or admin event"] --> Backend["Backend"]
    Backend --> Store[("Notifications in database")]
    Store --> Bell["Notification bell"]
    Bell --> User["User or admin views notifications"]
```

Notifications are stored in the database and shown in the app, where users can
see unread notifications and mark them as read.

## 7. Admin and background jobs

```mermaid
flowchart LR
    Admin["Admin"] --> Dashboard["Admin dashboard"]
    Dashboard --> Backend["Admin API and services"]
    Backend --> Actions["Review alerts<br/>Manage wallets<br/>Monitor transfers"]
    Actions --> Audit[("Audit log")]

    Scheduler["Scheduled jobs"] --> Settlement["Settle due transfers"]
    Scheduler --> Unlock["Unlock expired accounts"]
    Scheduler --> Summary["Create daily summary<br/>and reconcile ledger"]
    Settlement --> Database[("Application database")]
    Unlock --> Database
    Summary --> Database
```

Admins use the dashboard to monitor the ledger and take administrative
actions. Background jobs handle settlement, account unlocks, and daily
summaries automatically.
