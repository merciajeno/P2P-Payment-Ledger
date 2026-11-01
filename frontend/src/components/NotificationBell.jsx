import { useEffect, useRef, useState } from "react";
import { fetchNotifications, fetchUnreadCount, markNotificationsRead } from "../api";

export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const panelRef = useRef(null);

  // Poll unread count every 30 seconds
  useEffect(() => {
    let cancelled = false;

    async function loadCount() {
      const count = await fetchUnreadCount();
      if (!cancelled && count !== null) setUnreadCount(count);
    }

    loadCount();
    const interval = setInterval(loadCount, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  // Close on outside CLICK — not mousedown
  // mousedown fires before onClick so using it causes the panel
  // to close before it ever opens
  useEffect(() => {
    if (!open) return;

    function handleClickOutside(e) {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setOpen(false);
      }
    }

    // Use click, not mousedown
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [open]); // only attach listener when panel is open

  async function handleOpen() {
    if (open) {
      setOpen(false);
      return;
    }

    setOpen(true);
    setLoading(true);
    setError(null);

    try {
      const data = await fetchNotifications();
      if (!Array.isArray(data)) {
        throw new Error("The server returned an invalid notifications response.");
      }
      setNotifications(data);

      if (unreadCount > 0) {
        await markNotificationsRead();
        setUnreadCount(0);
      }
    } catch (e) {
      setError("Could not load notifications.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="notif-bell-wrapper" ref={panelRef}>
      <button
        type="button"
        className="notif-bell-button"
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
        aria-expanded={open}
        onClick={handleOpen}
      >
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && (
          <span className="notif-badge" aria-hidden="true">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="notif-panel" role="region" aria-label="Notifications">
          <div className="notif-panel-header">
            <span>Notifications</span>
          </div>

          {loading ? (
            <p className="notif-empty">Loading...</p>
          ) : error ? (
            <p className="notif-empty">{error}</p>
          ) : notifications.length === 0 ? (
            <p className="notif-empty">No notifications yet.</p>
          ) : (
            <ul className="notif-list">
              {notifications.map((n) => (
                <li
                  key={n.id}
                  className={`notif-item${n.read ? "" : " notif-item-unread"}`}
                >
                  <span className="notif-message">{n.message}</span>
                  <time className="notif-time">
                    {new Date(n.createdAt).toLocaleString(undefined, {
                      month: "short",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </time>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
