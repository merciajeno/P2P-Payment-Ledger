import { useEffect, useRef, useState } from "react";
import { submitTransfer } from "../api";
import Field from "./Field";

const PENDING_TRANSFER_KEY = "p2pLedgerPendingTransfer";
const TRANSACTION_STATUSES = new Set(["PENDING", "SUCCESS", "FAILED", "DECLINED"]);

export default function SendMoney({ onBack, onBalanceChange, onSessionExpired }) {
  const [receiverEmail, setReceiverEmail] = useState("");
  const [amount, setAmount] = useState("");
  const [notice, setNotice] = useState(null);
  const [pendingPayment, setPendingPayment] = useState(null);
  const [transactionStatus, setTransactionStatus] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRecovering, setIsRecovering] = useState(true);
  const [isReviewing, setIsReviewing] = useState(false);
  const submissionLock = useRef(false);

  useEffect(() => {
    try {
      const savedRequest = window.sessionStorage.getItem(PENDING_TRANSFER_KEY);
      if (savedRequest) {
        const restored = JSON.parse(savedRequest);
        const requestBody = JSON.parse(restored.body);
        if (
          typeof restored.requestId !== "string" ||
          requestBody.requestId !== restored.requestId ||
          typeof requestBody.receiverEmail !== "string" ||
          typeof requestBody.amount !== "number"
        ) {
          throw new Error("The saved payment request is invalid. It has not been changed.");
        }
        setPendingPayment(restored);
        setReceiverEmail(requestBody.receiverEmail);
        setAmount(String(requestBody.amount));
        setNotice({
          type: "error",
          text: "A payment has an unknown result. Retry it to check its status; the same request will be reused.",
        });
      }
    } catch (error) {
      setNotice({
        type: "error",
        text:
          error instanceof DOMException
            ? "Could not read the saved payment from this browser session."
            : error.message,
      });
    } finally {
      setIsRecovering(false);
    }
  }, []);

  async function sendPayment(payment) {
    if (submissionLock.current) {
      return;
    }

    submissionLock.current = true;
    setIsSubmitting(true);
    setNotice(null);

    try {
      const result = await submitTransfer(payment.body);
      if (result.definitiveError) {
        window.sessionStorage.removeItem(PENDING_TRANSFER_KEY);
        setPendingPayment(null);
        setIsReviewing(false);
        if (result.status === 401 || result.status === 403) {
          onSessionExpired();
          return;
        }
        setNotice({ type: "error", text: result.message });
        return;
      }

      if (result.uncertainError) {
        setNotice({
          type: "error",
          text: `${result.message} Retry to safely check this same payment.`,
        });
        return;
      }

      if (
        result.requestId !== payment.requestId ||
        !TRANSACTION_STATUSES.has(result.status)
      ) {
        setNotice({
          type: "error",
          text: "The server response could not be verified. Retry to check this same payment.",
        });
        return;
      }

      window.sessionStorage.removeItem(PENDING_TRANSFER_KEY);
      setPendingPayment(null);
      setTransactionStatus(result.status);
      setIsReviewing(false);
      setReceiverEmail("");
      setAmount("");
      if (Number.isFinite(result.balance)) {
        onBalanceChange(result.balance);
      }
      setNotice(null);
    } catch (error) {
      setNotice({
        type: "error",
        text: `${error.message} Retry to safely check this same payment.`,
      });
    } finally {
      submissionLock.current = false;
      setIsSubmitting(false);
    }
  }

  function handleReview(event) {
    event.preventDefault();
    setNotice(null);
    setIsReviewing(true);
  }

  function handleConfirm() {
    if (pendingPayment) {
      void sendPayment(pendingPayment);
      return;
    }

    const requestId = crypto.randomUUID();
    const body = JSON.stringify({
      requestId,
      receiverEmail: receiverEmail.trim(),
      amount: Number(amount),
    });
    const payment = { requestId, body };

    try {
      window.sessionStorage.setItem(PENDING_TRANSFER_KEY, JSON.stringify(payment));
    } catch {
      setNotice({
        type: "error",
        text: "Could not save this payment for safe retry. No payment was sent.",
      });
      return;
    }

    setPendingPayment(payment);
    setTransactionStatus(null);
    void sendPayment(payment);
  }

  function startNewPayment() {
    setTransactionStatus(null);
    setNotice(null);
    setReceiverEmail("");
    setAmount("");
    setIsReviewing(false);
  }

  return (
    <section className="wallet-action-panel form-panel" aria-labelledby="send-money-title">
      <button className="back-button" type="button" onClick={onBack}>
        ← Back to wallet
      </button>
      <div className="form-kicker">TRANSFER</div>
      <h1 id="send-money-title">Send money</h1>
      <p className="send-money-description">
        Enter the recipient’s registered email address and the amount to send.
      </p>
      {isRecovering ? (
        <p className="wallet-loading" role="status">Checking for a pending payment…</p>
      ) : pendingPayment ? (
        <div className="auth-form">
          <p>
            Retry the saved payment to recover its result. The request ID and payment details
            will remain unchanged.
          </p>
          <button
            className="submit-button"
            type="button"
            disabled={isSubmitting}
            onClick={() => void sendPayment(pendingPayment)}
          >
            {isSubmitting ? "Checking payment…" : "Retry payment"}
          </button>
        </div>
      ) : isReviewing ? (
        <div className="auth-form">
          <p>Send <strong>{Number(amount).toFixed(2)}</strong> to <strong>{receiverEmail.trim()}</strong>?</p>
          <button
            className="submit-button"
            type="button"
            disabled={isSubmitting}
            onClick={handleConfirm}
          >
            {isSubmitting ? "One moment…" : "Confirm payment"}
          </button>
          <button
            className="back-button"
            type="button"
            disabled={isSubmitting}
            onClick={() => setIsReviewing(false)}
          >
            Edit payment
          </button>
        </div>
      ) : (
        <form className="auth-form" onSubmit={handleReview}>
          <Field
            autoComplete="email"
            label="Recipient email"
            maxLength={254}
            onChange={(event) => setReceiverEmail(event.target.value)}
            placeholder="recipient@example.com"
            required
            type="email"
            value={receiverEmail}
          />
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
          <button className="submit-button" type="submit">
            Review payment
          </button>
        </form>
      )}
      {transactionStatus && (
        <div
          className={`notice ${
            transactionStatus === "FAILED" || transactionStatus === "DECLINED"
              ? "notice-error"
              : "notice-success"
          }`}
          role="status"
          aria-live="polite"
        >
          Transaction status: {transactionStatus}
        </div>
      )}
      {notice && (
        <div className={`notice notice-${notice.type}`} role="status" aria-live="polite">
          <span aria-hidden="true">{notice.type === "success" ? "✓" : "!"}</span>
          {notice.text}
        </div>
      )}
      {!pendingPayment && !isReviewing && transactionStatus && (
        <button className="back-button" type="button" onClick={startNewPayment}>
          Start a new payment
        </button>
      )}
    </section>
  );
}
