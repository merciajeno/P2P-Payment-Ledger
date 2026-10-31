import { useEffect, useRef, useState } from "react";
import { submitTransfer } from "../api";
import Field from "./Field";

const STORAGE_KEY = "p2pLedgerPendingTransfer";

const VALID_STATUSES = ["PENDING", "SUCCESS", "FAILED", "DECLINED"];

export default function SendMoney({ onBack, onBalanceChange, onSessionExpired }) {
  const [receiverEmail, setReceiverEmail] = useState("");
  const [amount, setAmount] = useState("");
  const [pin, setPin] = useState("");
  const [payment, setPayment] = useState(null);
  const [status, setStatus] = useState(null);
  const [notice, setNotice] = useState("");
  const [reviewing, setReviewing] = useState(false);
  const [enteringPin, setEnteringPin] = useState(false);  // new stage
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const lock = useRef(false);

  useEffect(() => {
    try {
      const saved = sessionStorage.getItem(STORAGE_KEY);
      if (saved) {
        const restored = JSON.parse(saved);
        const body = JSON.parse(restored.body);

        if (
          typeof restored.requestId !== "string" ||
          body.requestId !== restored.requestId ||
          typeof body.receiverEmail !== "string" ||
          typeof body.amount !== "number"
        ) {
          throw new Error("Saved payment data is invalid.");
        }

        setPayment(restored);
        setReceiverEmail(body.receiverEmail);
        setAmount(String(body.amount));
        setNotice("An earlier payment has an unknown result. Retry it to recover its status.");
      }
    } catch (error) {
      setNotice(error.message);
    } finally {
      setLoading(false);
    }
  }, []);

  async function sendPayment(currentPayment) {
    if (lock.current) return;
    lock.current = true;
    setSubmitting(true);
    setNotice("");

    try {
      const result = await submitTransfer(currentPayment.body);

      if (result.definitiveError) {
        sessionStorage.removeItem(STORAGE_KEY);
        setPayment(null);
        if (result.status === 401 || result.status === 403) {
          onSessionExpired();
          return;
        }
        setNotice(result.message);
        return;
      }

      if (result.uncertainError) {
        setNotice(`${result.message} Please retry the same payment.`);
        return;
      }

      if (
        result.requestId !== currentPayment.requestId ||
        !VALID_STATUSES.includes(result.transactionStatus)
      ) {
        setNotice("Could not verify the payment response. Retry the same payment.");
        return;
      }

      sessionStorage.removeItem(STORAGE_KEY);
      setPayment(null);
      setStatus(result.transactionStatus);
      setReceiverEmail("");
      setAmount("");
      setPin("");
      setReviewing(false);
      setEnteringPin(false);

      if (Number.isFinite(result.balance)) {
        onBalanceChange(result.balance);
      }
    } catch (error) {
      setNotice(`${error.message} Retry the same payment.`);
    } finally {
      lock.current = false;
      setSubmitting(false);
    }
  }

  function confirmPin() {
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
      setNotice("PIN must be exactly 4 digits.");
      return;
    }

    const requestId = crypto.randomUUID();

    const body = JSON.stringify({
      requestId,
      receiverEmail: receiverEmail.trim(),
      amount: Number(amount),
      pin,  // included in body sent to backend
    });

    const newPayment = { requestId, body };

    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(newPayment));
    } catch {
      setNotice("Could not save payment details. No payment was sent.");
      return;
    }

    setPayment(newPayment);
    setStatus(null);
    sendPayment(newPayment);
  }

  function startNewPayment() {
    setPayment(null);
    setStatus(null);
    setNotice("");
    setReceiverEmail("");
    setAmount("");
    setPin("");
    setReviewing(false);
    setEnteringPin(false);
  }

  if (loading) return <p role="status">Checking pending payments...</p>;

  return (
    <section className="wallet-action-panel form-panel">
      <button type="button" className="back-button" onClick={onBack}>
        ← Back to wallet
      </button>

      <div className="form-kicker">TRANSFER</div>
      <h1>Send money</h1>

      {/* ── Stage: retry an uncertain payment ── */}
      {payment ? (
        <div className="auth-form">
          <p>An earlier payment needs to be checked. Its request ID and payment details are preserved.</p>
          <button
            type="button"
            className="submit-button"
            disabled={submitting}
            onClick={() => sendPayment(payment)}
          >
            {submitting ? "Checking..." : "Retry payment"}
          </button>
        </div>

      /* ── Stage: enter PIN to confirm ── */
      ) : enteringPin ? (
        <div className="auth-form">
          <p>
            Confirm sending <strong>₹{Number(amount).toFixed(2)}</strong> to{" "}
            <strong>{receiverEmail.trim()}</strong>
          </p>
          <Field
            label="Transaction PIN"
            type="password"
            inputMode="numeric"
            maxLength={4}
            value={pin}
            onChange={(e) => {
              // only allow digits
              const val = e.target.value.replace(/\D/g, "");
              setPin(val);
            }}
            placeholder="••••"
            required
          />
          <button
            type="button"
            className="submit-button"
            disabled={submitting || pin.length !== 4}
            onClick={confirmPin}
          >
            {submitting ? "Processing..." : "Confirm & Send"}
          </button>
          <button
            type="button"
            className="back-button"
            disabled={submitting}
            onClick={() => { setEnteringPin(false); setPin(""); setNotice(""); }}
          >
            Back
          </button>
        </div>

      /* ── Stage: review details ── */
      ) : reviewing ? (
        <div className="auth-form">
          <p>
            Send <strong>₹{Number(amount).toFixed(2)}</strong> to{" "}
            <strong>{receiverEmail.trim()}</strong>?
          </p>
          <button
            type="button"
            className="submit-button"
            onClick={() => { setReviewing(false); setEnteringPin(true); }}
          >
            Continue to PIN
          </button>
          <button
            type="button"
            className="back-button"
            onClick={() => setReviewing(false)}
          >
            Edit payment
          </button>
        </div>

      /* ── Stage: enter email + amount ── */
      ) : (
        <form
          className="auth-form"
          onSubmit={(e) => { e.preventDefault(); setNotice(""); setReviewing(true); }}
        >
          <Field
            label="Recipient email"
            type="email"
            value={receiverEmail}
            onChange={(e) => setReceiverEmail(e.target.value)}
            placeholder="recipient@example.com"
            required
          />
          <Field
            label="Amount (₹)"
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00"
            required
          />
          <button type="submit" className="submit-button">
            Review payment
          </button>
        </form>
      )}

      {status && (
        <div
          className={`notice ${
            status === "FAILED" || status === "DECLINED" ? "notice-error" : "notice-success"
          }`}
          role="status"
        >
          Transaction status: {status}
        </div>
      )}

      {notice && (
        <div className="notice notice-error" role="alert">
          {notice}
        </div>
      )}

      {!payment && status && (
        <button type="button" className="back-button" onClick={startNewPayment}>
          Start a new payment
        </button>
      )}
    </section>
  );
}
