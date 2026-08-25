import type { StoredSession } from '../auth/session'

/**
 * Same-origin messaging between the app's open tabs.
 *
 * Two jobs:
 *  - tell other tabs that TODOs changed, so they refresh immediately instead of
 *    waiting for their next poll;
 *  - hand a newly opened tab the session that sessionStorage cannot share.
 *
 * BroadcastChannel is not available everywhere (older Safari), so every call
 * degrades to a no-op and the app falls back to polling alone.
 */
export type ChannelMessage =
  | { type: 'todos-changed'; at: number }
  | { type: 'session-request' }
  | { type: 'session-offer'; session: StoredSession }
  | { type: 'signed-out' }

const CHANNEL_NAME = 'scheduleNote'

export function openChannel(): BroadcastChannel | null {
  return typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel(CHANNEL_NAME)
}

export function publish(channel: BroadcastChannel | null, message: ChannelMessage): void {
  channel?.postMessage(message)
}

/** How long a new tab waits for another tab to offer it a session. */
export const SESSION_HANDSHAKE_TIMEOUT_MS = 250
