import type { AuthSession } from "./types";

const AUTH_KEY = "shiptrack.auth";
const FLASH_KEY = "shiptrack.flash";

export function saveAuth(session: AuthSession, persistent: boolean) {
  clearAuth();
  const storage = persistent ? localStorage : sessionStorage;
  storage.setItem(AUTH_KEY, JSON.stringify(session));
}

export function getAuth(): AuthSession | null {
  const raw = sessionStorage.getItem(AUTH_KEY) ?? localStorage.getItem(AUTH_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    clearAuth();
    return null;
  }
}

export function clearAuth() {
  sessionStorage.removeItem(AUTH_KEY);
  localStorage.removeItem(AUTH_KEY);
}

export function setFlash(message: string) {
  sessionStorage.setItem(FLASH_KEY, message);
}

export function takeFlash(): string | null {
  const message = sessionStorage.getItem(FLASH_KEY);
  sessionStorage.removeItem(FLASH_KEY);
  return message;
}
