# Product: P2P Payment Ledger

A wallet and peer-to-peer money transfer application. Users manage a personal wallet and send money to other registered users. Administrators monitor the ledger, review fraud alerts, and manage wallets and settlements.

## Core Features

- **Auth**: Registration and JWT-based login with two roles: `USER` and `ADMIN`
- **Wallet**: View balance, deposit and withdraw simulated funds; wallets can be frozen/unfrozen
- **Transfers**: Send money to another user by email or phone, protected by a transaction PIN; transfers are held as pending and settled by a background job; failed settlements trigger a refund
- **Friend/Foe lists**: One-way lists that modify transfer rules
  - Friends: transfers over ₹8,000 skip the high-value fraud alert; daily outgoing limit is ₹75,000
  - Foes: transfers capped at ₹5,000 per transaction
  - A user cannot be both friend and foe
- **Fraud detection**: Duplicate-transfer detection, daily outgoing limit enforcement, high-value transfer alerting
- **Transaction history**: Paginated, filterable by date range, direction (sent/received), and status
- **Personal stats**: Lifetime sent/received totals and this month's transaction count
- **Notifications**: Stored in DB; surfaced via a notification bell; users can mark them read
- **Admin dashboard**: Settled debit/credit reconciliation, pending settlement totals, user search, wallet management, fraud alert review (block/dismiss), settlement job monitoring
- **Audit log**: All administrative actions are recorded
- **Scheduled jobs**: Pending transfer settlement, daily summary/reconciliation, automatic account unlock after lockout

## Business Rules

- Currency is implicitly INR (₹)
- A sender's funds are debited immediately on transfer creation; the receiver is only credited after successful settlement
- High-value threshold and daily limits differ based on friend/foe relationship
- Failed login attempts lock the account; a scheduler unlocks it after the lockout period
