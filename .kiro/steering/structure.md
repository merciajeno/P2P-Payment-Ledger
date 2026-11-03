# Project Structure

## Repository Layout

```
P2P-ledger/
├── src/                        # Spring Boot backend (Maven standard layout)
├── frontend/                   # React + Vite frontend
├── pom.xml                     # Maven build descriptor
├── mvnw / mvnw.cmd             # Maven wrapper scripts
├── architecture.md             # High-level flow diagrams (Mermaid)
└── README.md                   # Setup and run instructions
```

## Backend — `src/main/java/com/mercia/`

```
com/mercia/
├── P2PLedgerApplication.java       # Spring Boot entry point (@SpringBootApplication)
├── config/
│   ├── ApplicationConfig.java      # JWT SecretKey bean (reads SECRET_KEY env var)
│   └── SecurityConfig.java         # SecurityFilterChain, BCryptPasswordEncoder bean
├── controller/                     # REST controllers — one per domain
│   ├── AdminController.java        # /admin/**
│   ├── AuthController.java         # /login, /register, /logout, /me
│   ├── FoeController.java          # /users/foes/**
│   ├── FriendController.java       # /users/friends
│   ├── LedgerEntryController.java  # /api/ledger/history
│   ├── NotificationController.java # /notifications/**
│   ├── TransactionController.java  # /transactions, /transactions/{action}
│   ├── UserController.java         # /users/search
│   ├── UserStatsController.java    # /users/stats
│   └── WalletController.java       # /wallet/**
├── dto/                            # Request and response DTOs (no entity leakage)
├── enums/                          # Domain enumerations (Role, Status, TransactionStatus, etc.)
├── exception/
│   ├── ExcessAmountException.java
│   ├── InvalidPasswordException.java
│   ├── UserNotFoundException.java
│   └── MyGlobalExceptionHandler.java   # @RestControllerAdvice global error handler
├── filter/
│   └── JwtFilter.java              # Validates JWT, sets SecurityContext
├── model/                          # JPA entities (User, Wallet, Transaction, etc.)
├── repository/                     # Spring Data JPA repositories (one per entity)
├── scheduler/
│   ├── SettlementScheduler.java    # Settles pending transfers
│   ├── AccountUnlockScheduler.java # Unlocks locked accounts
│   └── DailySummaryScheduler.java  # Triggers daily reconciliation
└── service/                        # Business logic (17 services, one per domain)
```

## Frontend — `frontend/`

```
frontend/
├── index.html                  # Vite HTML entry point
├── vite.config.js              # Build config + dev proxy for all API paths
├── package.json
└── src/
    ├── main.jsx                # App root: routing, session management, role-based rendering
    ├── api.js                  # All Axios API calls — single source of truth for HTTP
    ├── styles.css              # Global CSS
    └── components/
        ├── AuthPage.jsx            # Login and registration forms
        ├── WalletPage.jsx          # Wallet balance view
        ├── WalletActions.jsx       # Deposit and withdrawal forms
        ├── SendMoney.jsx           # P2P transfer form
        ├── TransactionHistoryPage.jsx
        ├── UserStatsPage.jsx
        ├── FriendsPage.jsx
        ├── FoesPage.jsx
        ├── NotificationBell.jsx
        ├── AdminPage.jsx
        ├── MainNavigation.jsx
        ├── BrandMark.jsx
        ├── CardArtwork.jsx
        └── Field.jsx               # Reusable form field component
```

## Conventions

### Backend
- Controllers are thin — delegate all logic to services
- Services contain business rules; repositories are query-only
- DTOs are used at the controller boundary; entities are never returned directly from controllers
- Enumerations live in `enums/` and are shared across layers
- Custom exceptions are thrown by services and caught by `MyGlobalExceptionHandler`
- Lombok annotations (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.) are used on models and DTOs

### Frontend
- All HTTP calls go through the Axios instance in `api.js` — do not call `fetch` or create separate Axios instances
- Navigation is done by calling the `navigate(path)` function passed down as a prop — do not use `window.location.href`
- Global state (current user, wallet balance, active path) lives in `App` inside `main.jsx` and is passed down as props
- Session expiry is handled by checking for 401/403 responses in `api.js`; the caller receives `null` on auth failure rather than an error
- When adding a new API path prefix, add it to the proxy list in `vite.config.js`
