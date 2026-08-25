import { apiClient } from '../api/client'
import type { AuthenticatedUser } from '../types'
import { sha256Hex } from './crypto'
import type { StoredSession } from './session'

interface SessionPayload {
  token: string
  expiresAt: string
  user: AuthenticatedUser
}

export async function register(
  username: string,
  displayName: string,
  password: string,
): Promise<StoredSession> {
  const { data } = await apiClient.post<SessionPayload>('/auth/register', {
    username,
    displayName: displayName.trim() || null,
    password: await sha256Hex(password),
  })
  return data
}

export async function login(username: string, password: string): Promise<StoredSession> {
  const { data } = await apiClient.post<SessionPayload>('/auth/login', {
    username,
    password: await sha256Hex(password),
  })
  return data
}

/**
 * Replaces a user's password given only their username -- no current
 * password, no email link. `AuthController` documents the trade-off; this
 * just calls it.
 */
export async function changePassword(username: string, newPassword: string): Promise<void> {
  await apiClient.post('/auth/change-password', {
    username,
    newPassword: await sha256Hex(newPassword),
  })
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

export async function fetchCurrentUser(): Promise<AuthenticatedUser> {
  const { data } = await apiClient.get<AuthenticatedUser>('/auth/me')
  return data
}
