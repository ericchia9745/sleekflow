import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'

import { setAuthToken } from '../api/client'
import { openChannel, publish, SESSION_HANDSHAKE_TIMEOUT_MS } from '../realtime/channel'
import type { ChannelMessage } from '../realtime/channel'
import type { AuthenticatedUser } from '../types'
import * as authApi from './api'
import { clearSession, readSession, writeSession } from './session'
import type { StoredSession } from './session'

interface AuthState {
  user: AuthenticatedUser | null
  /** True until the stored session (or a sibling tab's) has been resolved. */
  initialising: boolean
  signIn: (username: string, password: string) => Promise<void>
  signUp: (username: string, displayName: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [initialising, setInitialising] = useState(true)
  const channelRef = useRef<BroadcastChannel | null>(null)

  const adopt = useCallback((session: StoredSession) => {
    writeSession(session)
    setAuthToken(session.token)
    setUser(session.user)
  }, [])

  const forget = useCallback(() => {
    clearSession()
    setAuthToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    const channel = openChannel()
    channelRef.current = channel
    let settled = false

    const stored = readSession()
    if (stored) {
      adopt(stored)
      settled = true
      setInitialising(false)
    }

    if (channel) {
      channel.onmessage = (event: MessageEvent<ChannelMessage>) => {
        const message = event.data
        if (message.type === 'session-request') {
          // Another tab just opened with nothing. Hand it ours.
          const current = readSession()
          if (current) publish(channel, { type: 'session-offer', session: current })
        } else if (message.type === 'session-offer' && !settled) {
          settled = true
          adopt(message.session)
          setInitialising(false)
        } else if (message.type === 'signed-out') {
          forget()
        }
      }
    }

    if (!stored) {
      // Ask the other tabs, and give up quickly if nobody answers.
      publish(channel, { type: 'session-request' })
      const timer = setTimeout(() => {
        if (!settled) setInitialising(false)
      }, SESSION_HANDSHAKE_TIMEOUT_MS)
      return () => {
        clearTimeout(timer)
        channel?.close()
      }
    }

    return () => channel?.close()
  }, [adopt, forget])

  const signIn = useCallback(
    async (username: string, password: string) => {
      adopt(await authApi.login(username, password))
    },
    [adopt],
  )

  const signUp = useCallback(
    async (username: string, displayName: string, password: string) => {
      adopt(await authApi.register(username, displayName, password))
    },
    [adopt],
  )

  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      // Sign out locally even if the call failed: the token may already be
      // invalid, and leaving the user apparently signed in would be worse.
      forget()
      publish(channelRef.current, { type: 'signed-out' })
    }
  }, [forget])

  const value = useMemo(
    () => ({ user, initialising, signIn, signUp, signOut }),
    [user, initialising, signIn, signUp, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside an AuthProvider')
  return context
}
