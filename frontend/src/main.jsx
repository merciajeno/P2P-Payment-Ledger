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

function BrandMark({ href, onClick }) {
  return (
    <a className="brand" href={href} onClick={onClick} aria-label="P2P Ledger home">
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

function WalletActions({ onBack, onBalanceChange, onSessionExpired }) {
  const [action, setAction] = useState("deposit");
  const [amount, setAmount] = useState("");
  const [notice, setNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setNotice(null);
    setIsSubmitting(true);

    try {
      const response = await fetch(`${API_BASE_URL}/transactions/${action}`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount: Number(amount) }),
      });

      if (response.status === 401 || response.status === 403) {
        onSessionExpired();
        return;
      }

      const responseBody = (await response.text()).trim();
      if (!response.ok) {
        throw new Error(responseBody || `Could not ${action} money. Please try again.`);
      }

      const balance = await fetchWalletBalance();
      if (balance === null) {
        onSessionExpired();
        return;
      }
      onBalanceChange(balance);
      setAmount("");
      setNotice({
        type: "success",
        text: `${action === "deposit" ? "Deposit" : "Withdrawal"} complete.`,
      });
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

function App() {
  const [mode, setMode] = useState(
    window.location.pathname === "/signup" ? "register" : "login",
  );
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
  const [path, setPath] = useState(window.location.pathname);

  const isRegister = mode === "register";
  const isAuthPage = path === "/" || path === "/login" || path === "/signup";
  const isWalletPage = path === "/wallet";
  const isWalletActionsPage = path === "/manage-money";

  useEffect(() => {
    function handlePopState() {
      const currentPath = window.location.pathname;
      setPath(currentPath);
      setMode(currentPath === "/signup" ? "register" : "login");
    }

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  function navigate(nextPath) {
    window.history.pushState({}, "", nextPath);
    setPath(nextPath);
    if (nextPath === "/signup") {
      setMode("register");
    } else if (nextPath === "/login" || nextPath === "/") {
      setMode("login");
    }
  }

  function handleSessionExpired() {
    setIsAuthenticated(false);
    setWalletBalance(null);
    navigate("/login");
    setNotice({
      type: "error",
      text: "Your session has expired. Please sign in again.",
    });
  }

  useEffect(() => {
    let isCurrent = true;
    setIsCheckingSession(true);

    fetchWalletBalance()
      .then((balance) => {
        if (!isCurrent) {
          return;
        }

        if (balance !== null) {
          setWalletBalance(balance);
          setIsAuthenticated(true);
          if (["/", "/login", "/signup"].includes(path)) {
            navigate("/wallet");
          } else if (!["/wallet", "/manage-money"].includes(path)) {
            navigate("/wallet");
          }
        } else {
          setIsAuthenticated(false);
          setWalletBalance(null);
          if (!["/", "/login", "/signup"].includes(path)) {
            navigate("/login");
          } else if (path === "/") {
            navigate("/login");
          }
        }
      })
      .catch((error) => {
        if (!isCurrent) {
          return;
        }
        setIsAuthenticated(false);
        setNotice({
          type: "error",
          text:
            error instanceof TypeError
              ? "Couldn’t reach the server. Check that the ledger API is running and try again."
              : error.message,
        });
        if (!["/", "/login", "/signup"].includes(path)) {
          navigate("/login");
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
  }, [path]);

  function changeMode(nextMode) {
    setNotice(null);
    navigate(nextMode === "register" ? "/signup" : "/login");
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
        navigate("/login");
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
        navigate("/wallet");
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
        <BrandMark
          href={isAuthenticated ? "/wallet" : "/login"}
          onClick={(event) => {
            event.preventDefault();
            navigate(isAuthenticated ? "/wallet" : "/login");
          }}
        />
        <nav className="primary-nav" aria-label="Main navigation">
          {!isAuthenticated && (
            <>
              <a
                className={path === "/login" ? "nav-link nav-link-active" : "nav-link"}
                href="/login"
                aria-current={path === "/login" ? "page" : undefined}
                onClick={(event) => {
                  event.preventDefault();
                  navigate("/login");
                  setNotice(null);
                }}
              >
                Login
              </a>
              <a
                className={path === "/signup" ? "nav-link nav-link-active" : "nav-link"}
                href="/signup"
                aria-current={path === "/signup" ? "page" : undefined}
                onClick={(event) => {
                  event.preventDefault();
                  navigate("/signup");
                  setNotice(null);
                }}
              >
                Sign up
              </a>
            </>
          )}
          <a
            className={isWalletPage ? "nav-link nav-link-active" : "nav-link"}
            href="/wallet"
            aria-current={isWalletPage ? "page" : undefined}
            onClick={(event) => {
              event.preventDefault();
              navigate("/wallet");
            }}
          >
            Wallet
          </a>
          <a
            className={isWalletActionsPage ? "nav-link nav-link-active" : "nav-link"}
            href="/manage-money"
            aria-current={isWalletActionsPage ? "page" : undefined}
            onClick={(event) => {
              event.preventDefault();
              navigate("/manage-money");
            }}
          >
            Manage money
          </a>
        </nav>
        <div className="topbar-note">
          <span className="secure-icon" aria-hidden="true">✳</span>
          <span>YOUR MONEY, IN MOTION</span>
        </div>
      </header>

      {isCheckingSession ? (
        <section className="wallet-loading" role="status">Checking your session…</section>
      ) : isAuthenticated && isWalletActionsPage ? (
        <WalletActions
          onBack={() => navigate("/wallet")}
          onBalanceChange={setWalletBalance}
          onSessionExpired={handleSessionExpired}
        />
      ) : isAuthenticated && isWalletPage ? (
        <section className="wallet-panel form-panel" aria-labelledby="wallet-title">
          <div className="form-kicker">YOUR WALLET</div>
          <h1 id="wallet-title">Available balance</h1>
          <p className="wallet-balance" aria-live="polite">
            {walletBalance.toLocaleString(undefined, {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </p>
          <button
            className="submit-button wallet-action-link"
            type="button"
            onClick={() => navigate("/manage-money")}
          >
            Deposit or withdraw
          </button>
        </section>
      ) : isAuthPage ? (
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
      ) : (
        <section className="wallet-loading" role="status">Redirecting…</section>
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
