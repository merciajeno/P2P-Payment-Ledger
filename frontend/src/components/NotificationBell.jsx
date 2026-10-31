import { useEffect, useRef, useState } from "react";
import { fetchNotifications, fetchUnreadCount, markNotificationsRead } from "../api";

export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
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
    return () => { cancelled = true; clearInterval(interval); };
  }, []);

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(e) {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function handleOpen() {
    if (open) { setOpen(false); return; }

    setOpen(true);
    setLoading(true);
    try {
      const data = await fetchNotifications();
      setNotifications(data);
      // Mark as read after opening
      if (unreadCount > 0) {
        await markNotificationsRead();
        setUnreadCount(0);
      }
    } catch {
      // fail silently — panel just shows empty
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
          ) : notifications.length === 0 ? (
            <p className="notif-empty">No notifications yet.</p>
          ) : (
            <ul className="notif-list">
              {notifications.map((n) => (
                <li key={n.id} className={`notif-item${n.read ? "" : " notif-item-unread"}`}>
                  <span className="notif-message">{n.message}</span>
                  <time className="notif-time">
                    {new Date(n.createdAt).toLocaleString(undefined, {
                      month: "short", day: "numeric",
                      hour: "2-digit", minute: "2-digit",
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
