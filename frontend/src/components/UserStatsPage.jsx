import { useEffect, useState } from "react";
import { fetchUserStats } from "../api";

function formatAmount(amount) {
  return `₹${Number(amount).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export default function UserStatsPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    fetchUserStats()
      .then((result) => {
        if (active) setStats(result);
      })
      .catch((requestError) => {
        if (active) setError(requestError.message);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="user-stats-page" aria-labelledby="user-stats-title">
      <div className="form-kicker">YOUR ACTIVITY</div>
      <div className="transaction-history-heading">
        <div>
          <h1 id="user-stats-title">Personal stats</h1>
          <p>Successful transfer totals and your activity this month.</p>
        </div>
      </div>

      {error ? (
        <p className="ledger-history-message" role="alert">{error}</p>
      ) : !stats ? (
        <p className="ledger-history-message" role="status">Loading your statistics…</p>
      ) : (
        <div className="user-stats-grid">
          <StatCard label="Total sent" value={formatAmount(stats.totalSent)} detail="Successfully settled transfers" />
          <StatCard label="Total received" value={formatAmount(stats.totalReceived)} detail="Successfully settled transfers" />
          <StatCard label="Transactions this month" value={stats.transactionsThisMonth} detail="Sent and received, including pending or failed" />
        </div>
      )}
    </section>
  );
}

function StatCard({ label, value, detail }) {
  return (
    <article className="user-stat-card">
      <span className="user-stat-label">{label}</span>
      <strong className="user-stat-value">{value}</strong>
      <span className="user-stat-detail">{detail}</span>
    </article>
  );
}
