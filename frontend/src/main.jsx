import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import AuthPage from "./components/AuthPage";
import MainNavigation from "./components/MainNavigation";
import SendMoney from "./components/SendMoney";
import TransactionHistoryPage from "./components/TransactionHistoryPage";
import WalletActions from "./components/WalletActions";
import WalletPage from "./components/WalletPage";
import { fetchWalletBalance } from "./api";
import "./styles.css";

function getErrorMessage(error) {
  return error instanceof TypeError
    ? "Couldn’t reach the server. Check that the ledger API is running and try again."
    : error.message;
}

function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [walletBalance, setWalletBalance] = useState(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [notice, setNotice] = useState(null);
  const isAuthPage = ["/", "/login", "/signup"].includes(path);
  const isProtectedPage = ["/wallet", "/manage-money", "/send-money", "/history"].includes(path);

  function navigate(nextPath) {
    if (window.location.pathname !== nextPath) {
      window.history.pushState({}, "", nextPath);
    }
    setPath(nextPath);
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
    function handlePopState() {
      setPath(window.location.pathname);
    }

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

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
          setNotice(null);
          if (isAuthPage) {
            navigate("/wallet");
          } else if (!isProtectedPage) {
            navigate("/wallet");
          }
          return;
        }

        setIsAuthenticated(false);
        setWalletBalance(null);
        if (isProtectedPage || !isAuthPage) {
          navigate("/login");
        } else if (path === "/") {
          navigate("/login");
        }
      })
      .catch((error) => {
        if (!isCurrent) {
          return;
        }

        setIsAuthenticated(false);
        setNotice({ type: "error", text: getErrorMessage(error) });
        if (isProtectedPage || !isAuthPage) {
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

  function handleLoginSuccess(balance) {
    setWalletBalance(balance);
    setIsAuthenticated(true);
    setNotice(null);
    navigate("/wallet");
  }

  function handleRegisterSuccess(message) {
    setNotice({
      type: "success",
      text: message || "Account created. You can sign in now.",
    });
  }

  return (
    <main className="page-shell">
      <MainNavigation
        path={path}
        isAuthenticated={isAuthenticated}
        navigate={navigate}
        clearNotice={() => setNotice(null)}
      />

      {isCheckingSession ? (
        <section className="wallet-loading" role="status">Checking your session…</section>
      ) : isAuthenticated && path === "/manage-money" ? (
        <WalletActions
          onBack={() => navigate("/wallet")}
          onBalanceChange={setWalletBalance}
          onSessionExpired={handleSessionExpired}
        />
      ) : isAuthenticated && path === "/send-money" ? (
        <SendMoney
          onBack={() => navigate("/wallet")}
          onBalanceChange={setWalletBalance}
          onSessionExpired={handleSessionExpired}
        />
      ) : isAuthenticated && path === "/history" ? (
        <TransactionHistoryPage />
      ) : isAuthenticated && path === "/wallet" ? (
        <WalletPage
          balance={walletBalance}
          onManageMoney={() => navigate("/manage-money")}
        />
      ) : isAuthPage ? (
        <AuthPage
          mode={path === "/signup" ? "register" : "login"}
          notice={notice}
          setNotice={setNotice}
          onLoginSuccess={handleLoginSuccess}
          onRegisterSuccess={handleRegisterSuccess}
          navigate={navigate}
        />
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
