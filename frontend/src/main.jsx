import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

async function fetchWalletBalance() {
  const response = await fetch(`${API_BASE_URL}/wallet/balance`, {
    credentials: "include",
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }

  if (!response.ok) {
    const responseBody = (await response.text()).trim();
    throw new Error(responseBody || "Could not load your wallet. Please try again.");
  }

  const wallet = await response.json();
  if (!Number.isFinite(wallet.balance)) {
    throw new Error("The server returned an invalid wallet balance.");
  }

  return wallet.balance;
}

function BrandMark() {
  return (
    <a className="brand" href="/" aria-label="P2P Ledger home">
      <span className="brand-mark" aria-hidden="true">
        <span />
        <span />
      </span>
      <span>p2p<span className="brand-light">ledger</span></span>
    </a>
  );
}

function CardArtwork() {
  return (
    <div className="artwork" aria-hidden="true">
      <div className="artwork-orbit artwork-orbit-one" />
      <div className="artwork-orbit artwork-orbit-two" />
      <div className="floating-dot dot-one" />
      <div className="floating-dot dot-two" />
      <div className="floating-dot dot-three" />
      <div className="payment-card">
        <div className="payment-card-top">
          <span className="card-chip" />
          <span className="card-contactless">)))</span>
        </div>
        <div className="card-number">•••• &nbsp;•••• &nbsp;•••• &nbsp;2486</div>
        <div className="payment-card-bottom">
          <span>YOUR EVERYDAY, ELEVATED</span>
          <span className="card-logo">p.</span>
        </div>
        <div className="card-shine" />
      </div>
      <div className="artwork-caption">
        <span className="caption-dot" />
        <span>Money moves better together</span>
      </div>
    </div>
  );
}

function Field({ label, hint, ...inputProps }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <input {...inputProps} />
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

function App() {
  const [mode, setMode] = useState("login");
  const [login, setLogin] = useState({ email: "", password: "" });
  const [register, setRegister] = useState({
    name: "",
    email: "",
    phoneNumber: "",
    password: "",
    transactionPin: "",
  });
  const [notice, setNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [walletBalance, setWalletBalance] = useState(null);

  const isRegister = mode === "register";

  useEffect(() => {
    let isCurrent = true;

    fetchWalletBalance()
      .then((balance) => {
        if (isCurrent && balance !== null) {
          setWalletBalance(balance);
          setIsAuthenticated(true);
        }
      })
      .catch((error) => {
        if (isCurrent) {
          setNotice({
            type: "error",
            text:
              error instanceof TypeError
                ? "Couldn’t reach the server. Check that the ledger API is running and try again."
                : error.message,
          });
        }
      })
      .finally(() => {
        if (isCurrent) {
          setIsCheckingSession(false);
        }
      });

    return () => {
      isCurrent = false;
    };
  }, []);

  function changeMode(nextMode) {
    setMode(nextMode);
    setNotice(null);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setNotice(null);
    setIsSubmitting(true);

    const request = isRegister ? register : login;
    const endpoint = isRegister ? "/register" : "/login";

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      });
      const responseBody = (await response.text()).trim();

      if (!response.ok) {
        throw new Error(responseBody || "Something went wrong. Please try again.");
      }

      if (isRegister) {
        setLogin((current) => ({ ...current, email: register.email }));
        setMode("login");
        setNotice({
          type: "success",
          text: responseBody || "Account created. You can sign in now.",
        });
      } else {
        const balance = await fetchWalletBalance();
        if (balance === null) {
          throw new Error("Could not verify your session. Please sign in again.");
        }
        setWalletBalance(balance);
        setIsAuthenticated(true);
      }
    } catch (error) {
      setNotice({
        type: "error",
        text:
          error instanceof TypeError
            ? "Couldn’t reach the server. Check that the ledger API is running and try again."
            : error.message,
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="page-shell">
      <header className="topbar">
        <BrandMark />
        <div className="topbar-note">
          <span className="secure-icon" aria-hidden="true">✳</span>
          <span>YOUR MONEY, IN MOTION</span>
        </div>
      </header>

      {isCheckingSession ? (
        <section className="wallet-loading" role="status">Checking your session…</section>
      ) : isAuthenticated ? (
        <section className="wallet-panel form-panel" aria-labelledby="wallet-title">
          <div className="form-kicker">YOUR WALLET</div>
          <h1 id="wallet-title">Available balance</h1>
          <p className="wallet-balance" aria-live="polite">
            {walletBalance.toLocaleString(undefined, {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </p>
        </section>
      ) : (
        <section className={`auth-layout ${isRegister ? "register-layout" : ""}`}>
          <div className="intro-panel">
            <div className="eyebrow">
              <span className="eyebrow-line" />
              A BETTER WAY TO MOVE MONEY
            </div>
            <h1>
              Good things
              <br />
              happen when
              <br />
              money <span>moves.</span>
            </h1>
            <p className="intro-copy">
              Meet your money&apos;s new favorite place. Simple, secure,
              and ready when you are.
            </p>
            <CardArtwork />
            <div className="trust-note">
              <span className="trust-check" aria-hidden="true">✓</span>
              <span>Built for the way you live and send</span>
            </div>
          </div>

          <section className="form-panel" aria-labelledby="form-title">
            <div className="form-heading">
              <div className="form-kicker">WELCOME TO P2P LEDGER</div>
              <h2 id="form-title">{isRegister ? "Create your account" : "Welcome back"}</h2>
              <p>
                {isRegister
                  ? "A few details and you’re good to go."
                  : "Your next move is just a sign in away."}
              </p>
            </div>

            <div className="mode-switch" role="tablist" aria-label="Account access">
              <button
                className={!isRegister ? "mode-active" : ""}
                type="button"
                role="tab"
                aria-selected={!isRegister}
                onClick={() => changeMode("login")}
              >
                Sign in
              </button>
              <button
                className={isRegister ? "mode-active" : ""}
                type="button"
                role="tab"
                aria-selected={isRegister}
                onClick={() => changeMode("register")}
              >
                Create account
              </button>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
              {isRegister ? (
                <>
                  <Field
                    autoComplete="name"
                    label="Full name"
                    maxLength={100}
                    onChange={(event) =>
                      setRegister({ ...register, name: event.target.value })
                    }
                    placeholder="What should we call you?"
                    required
                    value={register.name}
                  />
                  <Field
                    autoComplete="email"
                    label="Email address"
                    maxLength={254}
                    onChange={(event) =>
                      setRegister({ ...register, email: event.target.value })
                    }
                    placeholder="you@example.com"
                    required
                    type="email"
                    value={register.email}
                  />
                  <Field
                    autoComplete="tel"
                    label="Phone number"
                    maxLength={16}
                    onChange={(event) =>
                      setRegister({ ...register, phoneNumber: event.target.value })
                    }
                    pattern="^\+?[1-9]\d{7,14}$"
                    placeholder="+14155552671"
                    required
                    type="tel"
                    value={register.phoneNumber}
                    hint="Include your country code, e.g. +14155552671"
                  />
                  <div className="field-row">
                    <Field
                      autoComplete="new-password"
                      label="Password"
                      maxLength={72}
                      minLength={8}
                      onChange={(event) =>
                        setRegister({ ...register, password: event.target.value })
                      }
                      placeholder="At least 8 characters"
                      required
                      type="password"
                      value={register.password}
                    />
                    <Field
                      autoComplete="off"
                      inputMode="numeric"
                      label="4-digit PIN"
                      maxLength={4}
                      onChange={(event) =>
                        setRegister({
                          ...register,
                          transactionPin: event.target.value.replace(/\D/g, "").slice(0, 4),
                        })
                      }
                      pattern="\d{4}"
                      placeholder="••••"
                      required
                      type="password"
                      value={register.transactionPin}
                    />
                  </div>
                </>
              ) : (
                <>
                  <Field
                    autoComplete="email"
                    label="Email address"
                    maxLength={254}
                    onChange={(event) =>
                      setLogin({ ...login, email: event.target.value })
                    }
                    placeholder="you@example.com"
                    required
                    type="email"
                    value={login.email}
                  />
                  <Field
                    autoComplete="current-password"
                    label="Password"
                    onChange={(event) =>
                      setLogin({ ...login, password: event.target.value })
                    }
                    placeholder="Enter your password"
                    required
                    type="password"
                    value={login.password}
                  />
                </>
              )}

              {notice && (
                <div className={`notice notice-${notice.type}`} role="status" aria-live="polite">
                  <span aria-hidden="true">{notice.type === "success" ? "✓" : "!"}</span>
                  {notice.text}
                </div>
              )}

              <button className="submit-button" type="submit" disabled={isSubmitting}>
                <span>{isSubmitting ? "One moment…" : isRegister ? "Create my account" : "Sign in to your account"}</span>
                {!isSubmitting && <span className="button-arrow" aria-hidden="true">↗</span>}
              </button>
            </form>

            <p className="form-footnote">
              By continuing, you agree to keep your account details safe and secure.
            </p>
          </section>
        </section>
      )}

      <footer className="page-footer">
        <span>© 2025 P2P Ledger</span>
        <span className="footer-divider" />
        <span>Made for money that moves</span>
      </footer>
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);
