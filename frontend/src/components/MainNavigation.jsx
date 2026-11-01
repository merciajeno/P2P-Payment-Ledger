import { useState } from "react";
import BrandMark from "./BrandMark";
import NotificationBell from "./NotificationBell";

export default function MainNavigation({ path, isAuthenticated, userRole, navigate, clearNotice, onLogout }) {
  const [logoutError, setLogoutError] = useState(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const isWalletPage = path === "/wallet";
  const isWalletActionsPage = path === "/manage-money";
  const isSendMoneyPage = path === "/send-money";
  const isHistoryPage = path === "/history";

  return (
    <header className="topbar">
      <BrandMark
        href={isAuthenticated ? (userRole === "ADMIN" ? "/admin" : "/wallet") : "/login"}
        onClick={(event) => {
          event.preventDefault();
          navigate(isAuthenticated ? (userRole === "ADMIN" ? "/admin" : "/wallet") : "/login");
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
                clearNotice();
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
                clearNotice();
              }}
            >
              Sign up
            </a>
          </>
        )}
        {isAuthenticated && userRole === "USER" && (
          <>
            <a
              className={isSendMoneyPage ? "nav-link nav-link-active" : "nav-link"}
              href="/send-money"
              aria-current={isSendMoneyPage ? "page" : undefined}
              onClick={(event) => {
                event.preventDefault();
                navigate("/send-money");
              }}
            >
              Send money
            </a>
            <a
              className={isHistoryPage ? "nav-link nav-link-active" : "nav-link"}
              href="/history"
              aria-current={isHistoryPage ? "page" : undefined}
              onClick={(event) => {
                event.preventDefault();
                navigate("/history");
              }}
            >
              History
            </a>
          </>
        )}
        {isAuthenticated && userRole === "USER" && (
          <>
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
          </>
        )}
        {isAuthenticated && userRole === "ADMIN" && (
          <a
            className={path === "/admin" ? "nav-link nav-link-active" : "nav-link"}
            href="/admin"
            aria-current={path === "/admin" ? "page" : undefined}
            onClick={(event) => {
              event.preventDefault();
              navigate("/admin");
            }}
          >
            Admin
          </a>
        )}
      </nav>
       <div className="topbar-right">
        {isAuthenticated && <NotificationBell />}
        <div className="topbar-note">
          <span className="secure-icon" aria-hidden="true">✳</span>
          <span>Notification</span>
        </div>
        {isAuthenticated && (
          <button
            type="button"
            className="logout-button"
            disabled={isLoggingOut}
            onClick={async () => {
              setLogoutError(null);
              setIsLoggingOut(true);
              try {
                await onLogout();
              } catch (error) {
                setLogoutError(error.message);
              } finally {
                setIsLoggingOut(false);
              }
            }}
          >
            {isLoggingOut ? "Logging out..." : "Log out"}
          </button>
        )}
        {logoutError && <p className="logout-error" role="alert">{logoutError}</p>}
        </div>
      <div className="topbar-note">
        <span className="secure-icon" aria-hidden="true">✳</span>
        <span>YOUR MONEY, IN MOTION</span>
      </div>
    </header>
  );
}
