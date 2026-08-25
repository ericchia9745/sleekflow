/**
 * Hashes a password in the browser so the plaintext never reaches the network.
 *
 * This is not a substitute for hashing on the server -- the value produced here
 * is what the server treats as the password, so on its own it would simply move
 * the problem. The server salts and hashes it again before storing it.
 *
 * `crypto.subtle` is only available in a secure context, which means HTTPS or
 * localhost. Anything else needs a TLS terminator in front of the app.
 */
export async function sha256Hex(value: string): Promise<string> {
  if (!globalThis.crypto?.subtle) {
    throw new Error(
      'Password hashing needs a secure context. Serve the app over HTTPS or use localhost.',
    )
  }
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
}
