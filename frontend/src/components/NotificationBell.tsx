"use client";

import { Bell } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, apiRequest } from "@/lib/api";
import { clearAuth } from "@/lib/auth";
import type { NotificationRecord } from "@/lib/types";

function formatTime(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function NotificationBell({ token }: { token: string }) {
  const router = useRouter();
  const containerRef = useRef<HTMLDivElement>(null);
  const [notifications, setNotifications] = useState<NotificationRecord[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  const loadNotifications = useCallback(async () => {
    try {
      const records = await apiRequest<NotificationRecord[]>("/notifications", {}, token);
      setNotifications(records);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        clearAuth();
        router.replace("/login");
      }
    } finally {
      setLoading(false);
    }
  }, [router, token]);

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadNotifications(), 0);
    const interval = window.setInterval(() => void loadNotifications(), 30000);
    return () => {
      window.clearTimeout(initialLoad);
      window.clearInterval(interval);
    };
  }, [loadNotifications]);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  async function openNotification(notification: NotificationRecord) {
    if (notification.unread) {
      try {
        const updated = await apiRequest<NotificationRecord>(
          `/notifications/${notification.id}/read`,
          { method: "PATCH" },
          token,
        );
        setNotifications((current) => current.map((item) => item.id === updated.id ? updated : item));
      } catch {
        return;
      }
    }
    setOpen(false);
    router.push(`/shipments/${notification.shipmentId}`);
  }

  const unreadCount = notifications.filter((notification) => notification.unread).length;

  return (
    <div className="notification-menu" ref={containerRef}>
      <button
        className="notification-bell"
        type="button"
        aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ""}`}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <Bell size={17} />
        {unreadCount > 0 && <span>{unreadCount > 99 ? "99+" : unreadCount}</span>}
      </button>

      {open && (
        <section className="notification-panel">
          <header><strong>Notifications</strong><span>{unreadCount} unread</span></header>
          <div className="notification-list">
            {loading ? (
              <p className="notification-empty">Loading...</p>
            ) : notifications.length === 0 ? (
              <p className="notification-empty">No notifications yet.</p>
            ) : notifications.map((notification) => (
              <button
                className={notification.unread ? "unread" : ""}
                type="button"
                key={notification.id}
                onClick={() => void openNotification(notification)}
              >
                <i aria-hidden="true" />
                <span>
                  <strong>{notification.title}</strong>
                  <small>{notification.message}</small>
                  <time>{formatTime(notification.createdAt)}</time>
                </span>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
