import axios from 'axios'

import { config } from '../config'

export const apiClient = axios.create({
  baseURL: config.apiBaseUrl,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * Held in a module variable rather than read from storage on every request:
 * the auth provider owns the token's lifetime and pushes changes here.
 */
let authToken: string | null = null

export function setAuthToken(token: string | null): void {
  authToken = token
}

apiClient.interceptors.request.use((request) => {
  if (authToken) {
    request.headers.Authorization = `Bearer ${authToken}`
  }
  return request
})

type UnauthorizedHandler = () => void

let onUnauthorized: UnauthorizedHandler = () => {}

/** Lets the app react to an expired or revoked session from anywhere. */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  onUnauthorized = handler
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    // A 401 on any call means the session is gone -- expired, revoked, or
    // signed out in another tab. Sending the user back to the sign-in screen
    // beats letting every subsequent request fail silently.
    if (status === 401 && !String(error.config?.url ?? '').startsWith('/auth/')) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)
