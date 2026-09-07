"use client";

import { LogOut } from "lucide-react";
import type { AuthSession } from "@/lib/types";
import { Brand } from "./Brand";
import { NotificationBell } from "./NotificationBell";

function roleLabel(role: string) {
  return role.toLowerCase().split("_").map((word) =>
    word.charAt(0).toUpperCase() + word.slice(1),
  ).join(" ");
}

export function AppHeader({
  session,
  section,
  onSignOut,
}: {
  session: AuthSession;
  section: string;
  onSignOut: () => void;
}) {
  const initials = session.user.fullName
    .split(" ")
    .slice(0, 2)
    .map((word) => word.charAt(0))
    .join("")
    .toUpperCase();

  return (
    <header className="topbar">
      <div className="topbar-brand">
        <Brand light href="/dashboard" />
        <span className="workspace-chip">{section}</span>
      </div>
      <div className="topbar-user">
        <NotificationBell token={session.token} />
        <span className="user-avatar" aria-hidden="true">{initials}</span>
        <div className="user-copy">
          <strong>{session.user.fullName}</strong>
          <span>{roleLabel(session.user.role)}</span>
        </div>
        <button className="secondary-button signout-button" type="button" onClick={onSignOut}>
          <LogOut size={15} /> Sign out
        </button>
      </div>
    </header>
  );
}
