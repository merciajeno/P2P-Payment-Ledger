export default function WalletPage({ balance, onManageMoney }) {
  return (
    <section className="wallet-panel form-panel" aria-labelledby="wallet-title">
      <div className="form-kicker">YOUR WALLET</div>
      <h1 id="wallet-title">Available balance</h1>
      <p className="wallet-balance" aria-live="polite">
        {balance.toLocaleString(undefined, {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })}
      </p>
      <button className="submit-button wallet-action-link" type="button" onClick={onManageMoney}>
        Deposit or withdraw
      </button>
    </section>
  );
}
