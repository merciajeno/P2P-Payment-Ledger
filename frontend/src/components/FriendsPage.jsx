import { useEffect, useState } from "react";
import { addFriend, fetchFriends, removeFriend } from "../api";
import Field from "./Field";

export default function FriendsPage() {
  const [friends, setFriends] = useState([]);
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(true);
  const [pendingAction, setPendingAction] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    fetchFriends()
      .then((result) => {
        if (active) setFriends(result);
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

  async function handleAddFriend(event) {
    event.preventDefault();
    setPendingAction("add");
    setError("");
    setNotice("");
    try {
      const friend = await addFriend(email.trim());
      setFriends((current) => [
        ...current.filter((existing) => existing.email !== friend.email),
        friend,
      ]);
      setEmail("");
      setNotice(`${friend.name} added as a friend and removed from your foes if previously listed.`);
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setPendingAction(null);
    }
  }

  async function handleRemoveFriend(friend) {
    setPendingAction(friend.email);
    setError("");
    setNotice("");
    try {
      await removeFriend(friend.email);
      setFriends((current) => current.filter((existing) => existing.email !== friend.email));
      setNotice(`${friend.name} removed from your friends.`);
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setPendingAction(null);
    }
  }

  return (
    <section className="form-panel friends-page">
      <div className="form-kicker">TRUSTED RECIPIENTS</div>
      <h1>Friends</h1>
      <p className="friends-description">
        Friend transfers over ₹8,000 won’t trigger a high-value fraud alert.
      </p>

      <form className="auth-form" onSubmit={handleAddFriend}>
        <Field
          label="Add a friend by email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="friend@example.com"
          required
        />
        <button
          type="submit"
          className="submit-button"
          disabled={pendingAction !== null || !email.trim()}
        >
          {pendingAction === "add" ? "Adding..." : "Add friend"}
        </button>
      </form>

      {error && <div className="notice notice-error" role="alert">{error}</div>}
      {notice && <div className="notice notice-success" role="status">{notice}</div>}

      <h2 className="friends-list-heading">Your friends</h2>
      {loading ? (
        <p role="status">Loading friends...</p>
      ) : friends.length === 0 ? (
        <p className="friends-empty">No friends added yet.</p>
      ) : (
        <ul className="friends-list">
          {friends.map((friend) => (
            <li className="recipient-result" key={friend.email}>
              <span className="recipient-avatar" aria-hidden="true">
                {friend.name.trim().charAt(0).toUpperCase()}
              </span>
              <span>
                <strong>{friend.name}</strong>
                <span>{friend.email}</span>
              </span>
              <button
                type="button"
                className="friend-remove-button"
                disabled={pendingAction !== null}
                onClick={() => handleRemoveFriend(friend)}
              >
                {pendingAction === friend.email ? "Removing..." : "Remove"}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
