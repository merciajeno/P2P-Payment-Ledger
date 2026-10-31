import { useEffect, useRef, useState } from "react";
import { submitTransfer } from "../api";
import Field from "./Field";

const STORAGE_KEY = "p2pLedgerPendingTransfer";

const VALID_STATUSES = [
  "PENDING",
  "SUCCESS",
  "FAILED",
  "DECLINED",
];

export default function SendMoney({
  onBack,
  onBalanceChange,
  onSessionExpired,
}) {
  const [receiverEmail, setReceiverEmail] = useState("");
  const [amount, setAmount] = useState("");
  const [payment, setPayment] = useState(null);
  const [status, setStatus] = useState(null);
  const [notice, setNotice] = useState("");
  const [reviewing, setReviewing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const lock = useRef(false);

  // 1. Recover an unfinished payment when the component loads
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

        setNotice(
          "An earlier payment has an unknown result. Retry it to recover its status."
        );
      }
    } catch (error) {
      setNotice(error.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // 2. Send or retry a payment
  async function sendPayment(currentPayment) {
    if (lock.current) return;

    lock.current = true;
    setSubmitting(true);
    setNotice("");

    try {
      const result = await submitTransfer(currentPayment.body);

      // The server has definitively rejected the request
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

      // We don't know whether the server processed the payment
      if (result.uncertainError) {
        setNotice(
          `${result.message} Please retry the same payment.`
        );
        return;
      }

      // Verify the response
      if (
        result.requestId !== currentPayment.requestId ||
        !VALID_STATUSES.includes(result.transactionStatus)
      ) {
        setNotice(
          "Could not verify the payment response. Retry the same payment."
        );
        return;
      }

      // Payment result successfully recovered
      sessionStorage.removeItem(STORAGE_KEY);

      setPayment(null);
      setStatus(result.status);
      setReceiverEmail("");
      setAmount("");
      setReviewing(false);

      if (Number.isFinite(result.balance)) {
        onBalanceChange(result.balance);
      }
    } catch (error) {
      // Network error: preserve the request for safe retry
      setNotice(
        `${error.message} Retry the same payment.`
      );
    } finally {
      lock.current = false;
      setSubmitting(false);
    }
  }

  // 3. Create a new payment with a unique request ID
  function confirmPayment() {
    if (payment) {
      sendPayment(payment);
      return;
    }

    const requestId = crypto.randomUUID();

    const body = JSON.stringify({
      requestId,
      receiverEmail: receiverEmail.trim(),
      amount: Number(amount),
    });

    const newPayment = { requestId, body };

    try {
      // Save BEFORE sending the request
      sessionStorage.setItem(
        STORAGE_KEY,
        JSON.stringify(newPayment)
      );
    } catch {
      setNotice(
        "Could not save payment details. No payment was sent."
      );
      return;
    }

    setPayment(newPayment);
    setStatus(null);

    sendPayment(newPayment);
  }

  // 4. Start a completely new payment
  function startNewPayment() {
    setPayment(null);
    setStatus(null);
    setNotice("");
    setReceiverEmail("");
    setAmount("");
    setReviewing(false);
  }

  if (loading) {
    return <p role="status">Checking pending payments...</p>;
  }

  return (
    <section className="wallet-action-panel form-panel">
      <button type="button" className="back-button" onClick={onBack}>
        ← Back to wallet
      </button>

      <div className="form-kicker">TRANSFER</div>
      <h1>Send money</h1>

      {payment ? (
        <div className="auth-form">
          <p>
            An earlier payment needs to be checked.
            Its request ID and payment details are preserved.
          </p>

          <button
            type="button"
            className="submit-button"
            disabled={submitting}
            onClick={() => sendPayment(payment)}
          >
            {submitting ? "Checking..." : "Retry payment"}
          </button>
        </div>
      ) : reviewing ? (
        <div className="auth-form">
          <p>
            Send <strong>{Number(amount).toFixed(2)}</strong> to{" "}
            <strong>{receiverEmail.trim()}</strong>?
          </p>

          <button
            type="button"
            className="submit-button"
            disabled={submitting}
            onClick={confirmPayment}
          >
            {submitting ? "Processing..." : "Confirm payment"}
          </button>

          <button
            type="button"
            className="back-button"
            disabled={submitting}
            onClick={() => setReviewing(false)}
          >
            Edit payment
          </button>
        </div>
      ) : (
        <form
          className="auth-form"
          onSubmit={(event) => {
            event.preventDefault();
            setNotice("");
            setReviewing(true);
          }}
        >
          <Field
            label="Recipient email"
            type="email"
            value={receiverEmail}
            onChange={(event) =>
              setReceiverEmail(event.target.value)
            }
            placeholder="recipient@example.com"
            required
          />

          <Field
            label="Amount"
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
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
            status === "FAILED" || status === "DECLINED"
              ? "notice-error"
              : "notice-success"
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
        <button
          type="button"
          className="back-button"
          onClick={startNewPayment}
        >
          Start a new payment
        </button>
      )}
    </section>
  );
}
