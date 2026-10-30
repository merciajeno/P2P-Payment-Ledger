import { useState } from "react";
import { submitAuthentication } from "../api";
import CardArtwork from "./CardArtwork";
import Field from "./Field";

function getErrorMessage(error) {
  return error instanceof TypeError
    ? "Couldn’t reach the server. Check that the ledger API is running and try again."
    : error.message;
}

export default function AuthPage({ mode, notice, setNotice, onLoginSuccess, onRegisterSuccess, navigate }) {
  const [login, setLogin] = useState({ email: "", password: "" });
  const [register, setRegister] = useState({
    name: "",
    email: "",
    phoneNumber: "",
    password: "",
    transactionPin: "",
  });
  const [formNotice, setFormNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isRegister = mode === "register";
  const visibleNotice = formNotice || notice;

  async function handleSubmit(event) {
    event.preventDefault();
    setFormNotice(null);
    setNotice(null);
    setIsSubmitting(true);

    try {
      const request = isRegister ? register : login;
      const result = await submitAuthentication(mode, request);
      if (isRegister) {
        setLogin((current) => ({ ...current, email: register.email }));
        navigate("/login");
        onRegisterSuccess(result.responseBody);
      } else {
        onLoginSuccess(result.balance);
      }
    } catch (error) {
      setFormNotice({ type: "error", text: getErrorMessage(error) });
    } finally {
      setIsSubmitting(false);
    }
  }

  function changeMode(nextMode) {
    setFormNotice(null);
    setNotice(null);
    navigate(nextMode === "register" ? "/signup" : "/login");
  }

  return (
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
                onChange={(event) => setRegister({ ...register, name: event.target.value })}
                placeholder="What should we call you?"
                required
                value={register.name}
              />
              <Field
                autoComplete="email"
                label="Email address"
                maxLength={254}
                onChange={(event) => setRegister({ ...register, email: event.target.value })}
                placeholder="you@example.com"
                required
                type="email"
                value={register.email}
              />
              <Field
                autoComplete="tel"
                label="Phone number"
                maxLength={16}
                onChange={(event) => setRegister({ ...register, phoneNumber: event.target.value })}
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
                  onChange={(event) => setRegister({ ...register, password: event.target.value })}
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
                onChange={(event) => setLogin({ ...login, email: event.target.value })}
                placeholder="you@example.com"
                required
                type="email"
                value={login.email}
              />
              <Field
                autoComplete="current-password"
                label="Password"
                onChange={(event) => setLogin({ ...login, password: event.target.value })}
                placeholder="Enter your password"
                required
                type="password"
                value={login.password}
              />
            </>
          )}

          {visibleNotice && (
            <div className={`notice notice-${visibleNotice.type}`} role="status" aria-live="polite">
              <span aria-hidden="true">{visibleNotice.type === "success" ? "✓" : "!"}</span>
              {visibleNotice.text}
            </div>
          )}

          <button className="submit-button" type="submit" disabled={isSubmitting}>
            <span>
              {isSubmitting
                ? "One moment…"
                : isRegister
                  ? "Create my account"
                  : "Sign in to your account"}
            </span>
            {!isSubmitting && <span className="button-arrow" aria-hidden="true">↗</span>}
          </button>
        </form>

        <p className="form-footnote">
          By continuing, you agree to keep your account details safe and secure.
        </p>
      </section>
    </section>
  );
}
