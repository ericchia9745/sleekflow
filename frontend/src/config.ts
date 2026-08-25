/** Runtime configuration, resolved from Vite environment variables. */
export const config = {
  /**
   * Base URL for API calls. Defaults to a same-origin `/api` prefix, which the
   * dev server proxies to the backend.
   */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
} as const
