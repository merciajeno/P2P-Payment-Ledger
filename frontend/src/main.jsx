import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import AdminPage from "./components/AdminPage";
import AuthPage from "./components/AuthPage";
import MainNavigation from "./components/MainNavigation";
import SendMoney from "./components/SendMoney";
import TransactionHistoryPage from "./components/TransactionHistoryPage";
import UserStatsPage from "./components/UserStatsPage";
import WalletActions from "./components/WalletActions";
import WalletPage from "./components/WalletPage";
import { fetchCurrentUser, fetchWalletBalance, logoutUser } from "./api";
import "./styles.css";

function getErrorMessage(error) {
  return error instanceof TypeError
    ? "Couldn’t reach the server. Check that the ledger API is running and try again."
    : error.message;
}

function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [currentUser, setCurrentUser] = useState(null);
  const [walletBalance, setWalletBalance] = useState(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [notice, setNotice] = useState(null);
  const isAuthPage = ["/", "/login", "/signup"].includes(path);
  const isProtectedPage = ["/admin", "/wallet", "/manage-money", "/send-money", "/history", "/stats"].includes(path);

  function navigate(nextPath) {
    if (window.location.pathname !== nextPath) {
      window.history.pushState({}, "", nextPath);
    }
    setPath(nextPath);
  }

  function handleSessionExpired() {
    setCurrentUser(null);
    setWalletBalance(null);
    navigate("/login");
    setNotice({
      type: "error",
      text: "Your session has expired. Please sign in again.",
    });
  }

  async function handleLogout() {
    await logoutUser();
    setCurrentUser(null);
    setWalletBalance(null);
    setNotice(null);
    navigate("/login");
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

    async function checkSession() {
      try {
        const user = await fetchCurrentUser();
        if (!isCurrent) {
          return;
        }

        if (user === null) {
          setCurrentUser(null);
          setWalletBalance(null);
          if (isProtectedPage || !isAuthPage || path === "/") {
            navigate("/login");
          }
          return;
        }

        const balance = user.role === "USER" ? await fetchWalletBalance() : null;
        if (isCurrent) {
          if (user.role === "USER" && balance === null) {
            setCurrentUser(null);
            setWalletBalance(null);
            navigate("/login");
            return;
          }

          setCurrentUser(user);
          setWalletBalance(balance);
          setNotice(null);
          if (user.role === "ADMIN") {
            if (isAuthPage || path !== "/admin") {
              navigate("/admin");
            }
          } else if (isAuthPage || !isProtectedPage || path === "/admin") {
            navigate("/wallet");
          }
        }
      } catch (error) {
        if (!isCurrent) {
          return;
        }

        setCurrentUser(null);
        setWalletBalance(null);
        setNotice({ type: "error", text: getErrorMessage(error) });
        if (isProtectedPage || !isAuthPage) {
          navigate("/login");
        }
      } finally {
        if (isCurrent) {
          setIsCheckingSession(false);
        }
      }
    }

    checkSession();

    return () => {
      isCurrent = false;
    };
  }, [path]);

  function handleLoginSuccess({ user, balance }) {
    setCurrentUser(user);
    setWalletBalance(balance);
    setNotice(null);
    navigate(user.role === "ADMIN" ? "/admin" : "/wallet");
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
        isAuthenticated={currentUser !== null}
        userRole={currentUser?.role}
        navigate={navigate}
        clearNotice={() => setNotice(null)}
        onLogout={handleLogout}
      />

      {isCheckingSession ? (
        <section className="wallet-loading" role="status">Checking your session…</section>
      ) : currentUser?.role === "ADMIN" && path === "/admin" ? (
        <AdminPage />
      ) : currentUser?.role === "USER" && path === "/manage-money" ? (
        <WalletActions
          onBack={() => navigate("/wallet")}
          onBalanceChange={setWalletBalance}
          onSessionExpired={handleSessionExpired}
        />
      ) : currentUser?.role === "USER" && path === "/send-money" ? (
        <SendMoney
          onBack={() => navigate("/wallet")}
          onBalanceChange={setWalletBalance}
          onSessionExpired={handleSessionExpired}
        />
      ) : currentUser?.role === "USER" && path === "/history" ? (
        <TransactionHistoryPage />
      ) : currentUser?.role === "USER" && path === "/stats" ? (
        <UserStatsPage />
      ) : currentUser?.role === "USER" && path === "/wallet" ? (
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
