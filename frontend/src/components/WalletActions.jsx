import { useState } from "react";
import { submitWalletAction } from "../api";
import Field from "./Field";

function getErrorMessage(error) {
  return error instanceof TypeError
    ? "Couldn’t reach the server. Check that the ledger API is running and try again."
    : error.message;
}

export default function WalletActions({ onBack, onBalanceChange, onSessionExpired }) {
  const [action, setAction] = useState("deposit");
  const [amount, setAmount] = useState("");
  const [notice, setNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setNotice(null);
    setIsSubmitting(true);

    try {
      const result = await submitWalletAction(action, Number(amount));
      if (result === null) {
        onSessionExpired();
        return;
      }

      onBalanceChange(result.balance);
      setAmount("");
      setNotice({
        type: "success",
        text: `${action === "deposit" ? "Deposit" : "Withdrawal"} complete.`,
      });
    } catch (error) {
      setNotice({ type: "error", text: getErrorMessage(error) });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="wallet-action-panel form-panel" aria-labelledby="wallet-action-title">
      <button className="back-button" type="button" onClick={onBack}>
        ← Back to wallet
      </button>
      <div className="form-kicker">WALLET</div>
      <h1 id="wallet-action-title">Manage your money</h1>
      <div className="mode-switch" role="tablist" aria-label="Choose wallet action">
        <button
          className={action === "deposit" ? "mode-active" : ""}
          type="button"
          role="tab"
          aria-selected={action === "deposit"}
          onClick={() => {
            setAction("deposit");
            setNotice(null);
          }}
        >
          Deposit
        </button>
        <button
          className={action === "withdraw" ? "mode-active" : ""}
          type="button"
          role="tab"
          aria-selected={action === "withdraw"}
          onClick={() => {
            setAction("withdraw");
            setNotice(null);
          }}
        >
          Withdraw
        </button>
      </div>
      <form className="auth-form" onSubmit={handleSubmit}>
        <Field
          autoComplete="off"
          label="Amount"
          min="0.01"
          onChange={(event) => setAmount(event.target.value)}
          placeholder="0.00"
          required
          step="0.01"
          type="number"
          value={amount}
        />
        {notice && (
          <div className={`notice notice-${notice.type}`} role="status" aria-live="polite">
            <span aria-hidden="true">{notice.type === "success" ? "✓" : "!"}</span>
            {notice.text}
          </div>
        )}
        <button className="submit-button" type="submit" disabled={isSubmitting}>
          <span>
            {isSubmitting
              ? "One moment…"
              : action === "deposit"
                ? "Deposit money"
                : "Withdraw money"}
          </span>
        </button>
      </form>
    </section>
  );
}
