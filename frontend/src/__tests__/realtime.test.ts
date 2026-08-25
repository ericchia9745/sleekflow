import { describe, expect, it, vi } from 'vitest'

import { openChannel, publish } from '../realtime/channel'

describe('cross-tab channel', () => {
  it('publishes a message when BroadcastChannel is available', () => {
    const postMessage = vi.fn()
    publish({ postMessage } as unknown as BroadcastChannel, { type: 'todos-changed', at: 1 })
    expect(postMessage).toHaveBeenCalledWith({ type: 'todos-changed', at: 1 })
  })

  it('is a no-op when the browser has no BroadcastChannel', () => {
    // Older Safari has none; the app must still work, falling back to polling.
    expect(() => publish(null, { type: 'signed-out' })).not.toThrow()
  })

  it('returns null instead of throwing when the API is missing', () => {
    const original = globalThis.BroadcastChannel
    // @ts-expect-error deliberately removing the API to model an older browser
    delete globalThis.BroadcastChannel
    try {
      expect(openChannel()).toBeNull()
    } finally {
      globalThis.BroadcastChannel = original
    }
  })
})
