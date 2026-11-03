import { useEffect, useState } from "react";
import { fetchLedgerHistory } from "../api";

function formatAmount(amount) {
  return Number(amount).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatLabel(value) {
  return value.toLowerCase().replaceAll("_", " ");
}

function formatStatus(value) {
  return value.charAt(0) + value.slice(1).toLowerCase();
}

function formatDate(value) {
  return new Date(value).toLocaleString();
}

export default function TransactionHistoryPage() {
  const [history, setHistory] = useState(null);
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ from: "", to: "", type: "ALL", status: "ALL" });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  function updateFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }));
    setPage(0);
  }

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);

    fetchLedgerHistory(page, filters)
      .then((result) => {
        if (active) {
          setHistory(result);
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.message);
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [page, filters]);

  return (
    <section className="transaction-history" aria-labelledby="transaction-history-title">
      <div className="form-kicker">ACCOUNT ACTIVITY</div>
      <div className="transaction-history-heading">
        <div>
          <h1 id="transaction-history-title">Transaction history</h1>
          <p>Wallet deposits, withdrawals, and transfers.</p>
        </div>
        {history && <span>{history.totalElements} entries</span>}
      </div>

      <div className="transaction-history-filters" aria-label="Filter transactions">
        <label>
          <span>From</span>
          <input
            type="date"
            aria-label="Start date"
            value={filters.from}
            max={filters.to || undefined}
            onChange={(event) => updateFilter("from", event.target.value)}
          />
        </label>
        <label>
          <span>To</span>
          <input
            type="date"
            aria-label="End date"
            value={filters.to}
            min={filters.from || undefined}
            onChange={(event) => updateFilter("to", event.target.value)}
          />
        </label>
        <label>
          <span>Type</span>
          <select value={filters.type} onChange={(event) => updateFilter("type", event.target.value)}>
            <option value="ALL">All types</option>
            <option value="SENT">Sent</option>
            <option value="RECEIVED">Received</option>
          </select>
        </label>
        <label>
          <span>Status</span>
          <select value={filters.status} onChange={(event) => updateFilter("status", event.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="SUCCESS">Success</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
        </label>
      </div>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {isLoading ? (
        <p className="ledger-history-message" role="status">Loading transactions…</p>
      ) : history?.content?.length ? (
        <>
          <div className="ledger-history-list">
            {history.content.map((entry) => (
              <article className="ledger-history-item" key={entry.id}>
                <div className="ledger-history-main">
                  <strong>{formatLabel(entry.reason)}</strong>
                  <span>{entry.description || formatLabel(entry.entryType)}</span>
                  {entry.remark && <span>Remark: {entry.remark}</span>}
                  <time dateTime={entry.createdAt}>{formatDate(entry.createdAt)}</time>
                  {entry.transferType && <span>{formatLabel(entry.transferType)}</span>}
                  {entry.status && (
                    <span className={`ledger-status ledger-status-${entry.status.toLowerCase()}`}>
                      Current status: {formatStatus(entry.status)}
                    </span>
                  )}
                  {entry.status === "PENDING" && entry.settlementDueAt && (
                    <span className="ledger-settlement-date">
                      Expected settlement: <time dateTime={entry.settlementDueAt}>{formatDate(entry.settlementDueAt)}</time>
                    </span>
                  )}
                </div>
                <div className="ledger-history-side">
                  <strong className={entry.entryType === "CREDIT" ? "ledger-credit" : "ledger-debit"}>
                    {entry.entryType === "CREDIT" ? "+" : "−"}
                    {formatAmount(entry.amount)}
                  </strong>
                  <span>{formatLabel(entry.entryType)}</span>
                </div>
              </article>
            ))}
          </div>
          <div className="ledger-history-pagination">
            <span>Page {history.number + 1} of {Math.max(history.totalPages, 1)}</span>
            <div>
              <button type="button" disabled={history.first} onClick={() => setPage((current) => current - 1)}>
                Previous
              </button>
              <button
                type="button"
                disabled={history.last || history.totalPages === 0}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : !error ? (
        <p className="ledger-history-message">No transactions match these filters.</p>
      ) : null}
    </section>
  );
}
