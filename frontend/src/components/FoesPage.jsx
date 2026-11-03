import { useEffect, useState } from "react";
import { addFoe, fetchFoes, removeFoe } from "../api";
import Field from "./Field";

export default function FoesPage() {
  const [foes, setFoes] = useState([]);
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [pendingAction, setPendingAction] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    fetchFoes()
      .then((result) => {
        if (active) setFoes(result);
      })
      .catch((loadError) => {
        if (active) setError(loadError.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  async function handleAddFoe(event) {
    event.preventDefault();
    setPendingAction("add");
    setError("");
    setNotice("");
    try {
      const foe = await addFoe(email.trim());
      setFoes((current) => [
        ...current.filter((existing) => existing.email !== foe.email),
        foe,
      ]);
      setEmail("");
      setNotice(`${foe.name} added as a foe and removed from your friends if previously listed.`);
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setPendingAction(null);
    }
  }

  async function handleRemoveFoe(foe) {
    setPendingAction(foe.email);
    setError("");
    setNotice("");
    try {
      await removeFoe(foe.email);
      setFoes((current) => current.filter((existing) => existing.email !== foe.email));
      setNotice(`${foe.name} removed from your foes.`);
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <section className="form-panel friends-page">
      <div className="form-kicker">FOE RECIPIENTS</div>
      <h1>Foes</h1>
      <p className="friends-description">
        Transfers to a foe cannot exceed ₹5,000 per transaction.
      </p>

      <form className="auth-form" onSubmit={handleAddFoe}>
        <Field
          label="Add a foe by email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="user@example.com"
          required
        />
        <button
          type="submit"
          className="submit-button"
          disabled={pendingAction !== null || !email.trim()}
        >
          {pendingAction === "add" ? "Adding..." : "Add foe"}
        </button>
      </form>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      <h2 className="friends-list-heading">Your foes</h2>
      {loading ? (
        <p role="status">Loading foes...</p>
      ) : foes.length === 0 ? (
        <p className="friends-empty">No foes added yet.</p>
      ) : (
        <ul className="friends-list">
          {foes.map((foe) => (
            <li className="recipient-result" key={foe.email}>
              <span className="recipient-avatar" aria-hidden="true">
                {foe.name.trim().charAt(0).toUpperCase()}
              </span>
              <span>
                <strong>{foe.name}</strong>
                <span>{foe.email}</span>
              </span>
              <button
                type="button"
                className="friend-remove-button"
                disabled={pendingAction !== null}
                onClick={() => handleRemoveFoe(foe)}
              >
                {pendingAction === foe.email ? "Removing..." : "Remove"}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
