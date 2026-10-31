import BrandMark from "./BrandMark";

export default function MainNavigation({ path, isAuthenticated, navigate, clearNotice }) {
  const isWalletPage = path === "/wallet";
  const isWalletActionsPage = path === "/manage-money";
  const isSendMoneyPage = path === "/send-money";
  const isHistoryPage = path === "/history";

  return (
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
        {isAuthenticated && (
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
  );
}
