import type { AuthenticatedUser } from '../types'

export interface StoredSession {
  token: string
  expiresAt: string
  user: AuthenticatedUser
}

const STORAGE_KEY = 'scheduleNote.session'

/**
 * The session lives in sessionStorage, so closing the tab ends it and it is
 * never written to disk. The cost is that a new tab starts with nothing, which
 * is what the cross-tab handshake in `realtime/channel.ts` exists to solve.
 */
export function readSession(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as StoredSession
    if (!session.token || !session.user) return null
    // A token past its expiry is worth discarding here rather than discovering
    // through a failed request.
    if (session.expiresAt && Date.parse(session.expiresAt) < Date.now()) return null
    return session
  } catch {
    return null
  }
}

export function writeSession(session: StoredSession): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}
