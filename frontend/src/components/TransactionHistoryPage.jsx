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

function formatDate(value) {
  return new Date(value).toLocaleString();
}

export default function TransactionHistoryPage() {
  const [history, setHistory] = useState(null);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);

    fetchLedgerHistory(page)
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
  }, [page]);

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
                  <time dateTime={entry.createdAt}>{formatDate(entry.createdAt)}</time>
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
        <p className="ledger-history-message">No transactions yet.</p>
      ) : null}
    </section>
  );
}
