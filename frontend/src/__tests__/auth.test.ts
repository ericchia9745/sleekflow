import { beforeEach, describe, expect, it } from 'vitest'

import { sha256Hex } from '../auth/crypto'
import { clearSession, readSession, writeSession } from '../auth/session'
import type { StoredSession } from '../auth/session'

const user = { id: 1, username: 'eric', displayName: 'Eric', createdAt: '2026-01-01T00:00:00Z' }

function session(overrides: Partial<StoredSession> = {}): StoredSession {
  return {
    token: 'a-token',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    user,
    ...overrides,
  }
}

describe('sha256Hex', () => {
  it('matches the reference digest the server expects', async () => {
    // Well-known SHA-256 of "abc". If this drifts, every password stops
    // verifying, so pin it against a value computed elsewhere.
    expect(await sha256Hex('abc')).toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    )
  })

  it('produces the documented 64-character hex form', async () => {
    expect(await sha256Hex('correct horse battery')).toMatch(/^[a-f0-9]{64}$/)
  })

  it('is stable for the same input and different for others', async () => {
    expect(await sha256Hex('same')).toBe(await sha256Hex('same'))
    expect(await sha256Hex('same')).not.toBe(await sha256Hex('Same'))
  })
})

describe('session storage', () => {
  beforeEach(() => {
    clearSession()
  })

  it('round-trips a session', () => {
    writeSession(session())
    expect(readSession()?.user.username).toBe('eric')
  })

  it('returns nothing when there is no session', () => {
    expect(readSession()).toBeNull()
  })

  it('discards a session that has already expired', () => {
    // Better to notice here than through a failed request.
    writeSession(session({ expiresAt: new Date(Date.now() - 1000).toISOString() }))
    expect(readSession()).toBeNull()
  })

  it('survives corrupt storage rather than throwing', () => {
    sessionStorage.setItem('scheduleNote.session', '{not json')
    expect(readSession()).toBeNull()
  })

  it('rejects a stored value that is missing its token', () => {
    sessionStorage.setItem('scheduleNote.session', JSON.stringify({ user }))
    expect(readSession()).toBeNull()
  })

  it('clears on sign-out', () => {
    writeSession(session())
    clearSession()
    expect(readSession()).toBeNull()
  })
})
