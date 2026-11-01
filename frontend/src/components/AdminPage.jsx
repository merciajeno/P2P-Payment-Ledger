import { useEffect, useState } from "react";
import {
  blockFraudAlert, dismissFraudAlert,
  fetchAdminDashboard, fetchAdminTransactions,
  fetchAdminUsers, fetchFraudAlerts,
  fetchPendingSettlements, fetchRecentJobs,
  freezeWallet, manualSettle, unfreezeWallet,
} from "../api";

const TABS = ["Dashboard", "Users", "Transactions", "Settlements", "Fraud Alerts", "Jobs"];

export default function AdminPage() {
  const [tab, setTab] = useState("Dashboard");

  return (
    <section className="admin-page">
      <header className="admin-page-header">
        <div>
          <div className="admin-eyebrow"><span /> ADMINISTRATION</div>
          <h1>Control center</h1>
          <p>Monitor activity and manage the ledger from one place.</p>
        </div>
        <span className="admin-access-label">Administrator access</span>
      </header>
      <div className="admin-tabs" role="tablist" aria-label="Admin sections">
        {TABS.map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={tab === t}
            className={`admin-tab${tab === t ? " admin-tab-active" : ""}`}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>
      <div className="admin-content" role="tabpanel" aria-label={`${tab} overview`}>
        {tab === "Dashboard"    && <AdminDashboard />}
        {tab === "Users"        && <AdminUsers />}
        {tab === "Transactions" && <AdminTransactions />}
        {tab === "Settlements"  && <AdminSettlements />}
        {tab === "Fraud Alerts" && <AdminFraudAlerts />}
        {tab === "Jobs"         && <AdminJobs />}
      </div>
    </section>
  );
}

// ── Dashboard ──────────────────────────────────────────────────
function AdminDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchAdminDashboard().then(setData).catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="admin-error" role="alert">{error}</p>;
  if (!data)  return <p className="admin-loading" role="status">Loading dashboard...</p>;

  return (
    <div className="admin-stats-grid">
      <StatCard label="Total users" value={data.totalUsers} detail="Registered accounts" />
      <StatCard label="Transfers initiated" value={data.todayTransfersInitiated} detail="Today" />
      <StatCard label="Transfers settled" value={data.todayTransfersSettled} detail="Today" />
      <StatCard label="Transaction volume" value={`₹${Number(data.todayTransactionVolume).toFixed(2)}`} detail="Today" />
      <StatCard label="Pending settlement" value={`₹${Number(data.totalPendingSettlementAmount).toFixed(2)}`} detail="Awaiting processing" />
      <StatCard label="Fraud alerts" value={data.pendingFraudAlertsCount} detail="Pending review" />
    </div>
  );
}

function StatCard({ label, value, detail }) {
  return (
    <div className="admin-stat-card">
      <span className="admin-stat-label">{label}</span>
      <span className="admin-stat-value">{value}</span>
      <span className="admin-stat-detail">{detail}</span>
    </div>
  );
}

// ── Users ──────────────────────────────────────────────────────
function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    fetchAdminUsers().then(setUsers).catch((e) => setError(e.message));
  }, []);

  async function handleFreeze(userId) {
    try {
      await freezeWallet(userId);
      setUsers((prev) => prev.map((u) =>
        u.userId === userId ? { ...u, walletStatus: "FROZEN" } : u));
      setMsg("Wallet frozen");
    } catch (e) { setError(e.message); }
  }

  async function handleUnfreeze(userId) {
    try {
      await unfreezeWallet(userId);
      setUsers((prev) => prev.map((u) =>
        u.userId === userId ? { ...u, walletStatus: "ACTIVE" } : u));
      setMsg("Wallet unfrozen");
    } catch (e) { setError(e.message); }
  }

  if (error) return <p className="admin-error" role="alert">{error}</p>;

  return (
    <>
      {msg && <p className="admin-success">{msg}</p>}
      <table className="admin-table">
        <thead>
          <tr>
            <th>Name</th><th>Email</th><th>Phone</th>
            <th>Balance</th><th>Wallet Status</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.length === 0 ? (
            <tr><td colSpan={6} className="admin-empty">No users found</td></tr>
          ) : users.map((u) => (
            <tr key={u.userId}>
              <td>{u.name}</td>
              <td>{u.email}</td>
              <td>{u.phoneNumber}</td>
              <td>₹{Number(u.walletBalance).toFixed(2)}</td>
              <td><StatusBadge status={u.walletStatus} /></td>
              <td>
                {u.walletStatus !== "FROZEN" ? (
                  <button className="admin-action-btn admin-btn-danger"
                    onClick={() => handleFreeze(u.userId)}>Freeze</button>
                ) : (
                  <button className="admin-action-btn admin-btn-success"
                    onClick={() => handleUnfreeze(u.userId)}>Unfreeze</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

// ── Transactions ───────────────────────────────────────────────
function AdminTransactions() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    setData(null);
    fetchAdminTransactions(page).then(setData).catch((e) => setError(e.message));
  }, [page]);

  if (error) return <p className="admin-error" role="alert">{error}</p>;
  if (!data)  return <p className="admin-loading" role="status">Loading transactions...</p>;

  return (
    <>
      <table className="admin-table">
        <thead>
          <tr>
            <th>ID</th><th>From</th><th>To</th><th>Amount</th>
            <th>Status</th><th>Created</th><th>Due</th>
          </tr>
        </thead>
        <tbody>
          {data.content.map((t) => (
            <tr key={t.id}>
              <td>{t.id}</td>
              <td>{t.senderName}</td>
              <td>{t.receiverName}</td>
              <td>₹{Number(t.amount).toFixed(2)}</td>
              <td><StatusBadge status={t.status} /></td>
              <td>{new Date(t.createdAt).toLocaleString()}</td>
              <td>{new Date(t.settlementDueAt).toLocaleDateString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="admin-pagination">
        <button disabled={data.first} onClick={() => setPage((p) => p - 1)}>Previous</button>
        <span>Page {data.number + 1} of {Math.max(data.totalPages, 1)}</span>
        <button disabled={data.last} onClick={() => setPage((p) => p + 1)}>Next</button>
      </div>
    </>
  );
}

// ── Settlements ────────────────────────────────────────────────
function AdminSettlements() {
  const [txns, setTxns] = useState([]);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    fetchPendingSettlements().then(setTxns).catch((e) => setError(e.message));
  }, []);

  async function handleSettle(id) {
    try {
      await manualSettle(id);
      setTxns((prev) => prev.filter((t) => t.id !== id));
      setMsg(`Transaction ${id} settled`);
    } catch (e) { setError(e.message); }
  }

  if (error) return <p className="admin-error" role="alert">{error}</p>;

  return (
    <>
      {msg && <p className="admin-success">{msg}</p>}
      <table className="admin-table">
        <thead>
          <tr>
            <th>ID</th><th>From</th><th>To</th>
            <th>Amount</th><th>Due</th><th>Action</th>
          </tr>
        </thead>
        <tbody>
          {txns.length === 0 ? (
            <tr><td colSpan={6} className="admin-empty">No pending settlements</td></tr>
          ) : txns.map((t) => (
            <tr key={t.id}>
              <td>{t.id}</td>
              <td>{t.senderName}</td>
              <td>{t.receiverName}</td>
              <td>₹{Number(t.amount).toFixed(2)}</td>
              <td>{new Date(t.settlementDueAt).toLocaleDateString()}</td>
              <td>
                <button className="admin-action-btn admin-btn-success"
                  onClick={() => handleSettle(t.id)}>Settle now</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

// ── Fraud Alerts ───────────────────────────────────────────────
function AdminFraudAlerts() {
  const [alerts, setAlerts] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchFraudAlerts().then(setAlerts).catch((e) => setError(e.message));
  }, []);

  async function handleBlock(alertId) {
    try {
      await blockFraudAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.alertId !== alertId));
    } catch (e) { setError(e.message); }
  }

  async function handleDismiss(alertId) {
    try {
      await dismissFraudAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.alertId !== alertId));
    } catch (e) { setError(e.message); }
  }

  if (error) return <p className="admin-error" role="alert">{error}</p>;

  return (
    <table className="admin-table">
      <thead>
        <tr>
          <th>Type</th><th>User</th><th>Amount</th>
          <th>Description</th><th>Created</th><th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {alerts.length === 0 ? (
          <tr><td colSpan={6} className="admin-empty">No pending fraud alerts</td></tr>
        ) : alerts.map((a) => (
          <tr key={a.alertId}>
            <td>{a.alertType.replaceAll("_", " ")}</td>
            <td>{a.walletOwnerName}</td>
            <td>₹{Number(a.amount).toFixed(2)}</td>
            <td>{a.description}</td>
            <td>{new Date(a.createdAt).toLocaleString()}</td>
            <td className="admin-actions-cell">
              {a.transactionId && (
                <button className="admin-action-btn admin-btn-danger"
                  onClick={() => handleBlock(a.alertId)}>Block</button>
              )}
              <button className="admin-action-btn"
                onClick={() => handleDismiss(a.alertId)}>Dismiss</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ── Jobs ───────────────────────────────────────────────────────
function AdminJobs() {
  const [jobs, setJobs] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchRecentJobs().then(setJobs).catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="admin-error" role="alert">{error}</p>;

  return (
    <table className="admin-table">
      <thead>
        <tr>
          <th>Started</th><th>Completed</th><th>Status</th>
          <th>Processed</th><th>Settled</th><th>Failed</th>
          <th>Declined</th><th>Amount</th>
        </tr>
      </thead>
      <tbody>
        {jobs.length === 0 ? (
          <tr><td colSpan={8} className="admin-empty">No job runs yet</td></tr>
        ) : jobs.map((j) => (
          <tr key={j.id}>
            <td>{new Date(j.startedAt).toLocaleString()}</td>
            <td>{j.completedAt ? new Date(j.completedAt).toLocaleString() : "—"}</td>
            <td><StatusBadge status={j.status} /></td>
            <td>{j.totalProcessed}</td>
            <td>{j.settledCount}</td>
            <td>{j.failedCount}</td>
            <td>{j.declinedCount}</td>
            <td>₹{Number(j.totalAmountSettled).toFixed(2)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// ── Shared ─────────────────────────────────────────────────────
function StatusBadge({ status }) {
  const colorMap = {
    ACTIVE: "badge-success", INACTIVE: "badge-warn",
    FROZEN: "badge-danger",  PENDING: "badge-warn",
    SUCCESS: "badge-success", FAILED: "badge-danger",
    DECLINED: "badge-danger", RUNNING: "badge-warn",
    PARTIAL_SUCCESS: "badge-warn",
  };
  return (
    <span className={`admin-badge ${colorMap[status] ?? ""}`}>
      {status}
    </span>
  );
}
