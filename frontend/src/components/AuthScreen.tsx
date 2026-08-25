import { useState } from 'react'

import { describeError } from '../api/todos'
import { useAuth } from '../auth/AuthContext'

type Mode = 'signIn' | 'signUp'

export function AuthScreen() {
  const { signIn, signUp } = useAuth()
  const [mode, setMode] = useState<Mode>('signIn')
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'signIn') {
        await signIn(username.trim(), password)
      } else {
        await signUp(username.trim(), displayName, password)
      }
    } catch (err) {
      setError(describeError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-screen">
      <form className="todo-form auth-form" onSubmit={submit}>
        <h2>{mode === 'signIn' ? 'Sign in' : 'Create an account'}</h2>

        {error && <p className="error" role="alert">{error}</p>}

        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            minLength={3}
            maxLength={60}
            pattern="[a-zA-Z0-9._\-]+"
            required
            autoFocus
          />
        </label>

        {mode === 'signUp' && (
          <label>
            Display name <span className="muted small">(optional)</span>
            <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={100} />
          </label>
        )}

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'signIn' ? 'current-password' : 'new-password'}
            minLength={8}
            required
          />
        </label>

        <p className="hint">
          The password is hashed in your browser before it is sent; the plaintext never leaves this page.
        </p>

        <div className="actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Working…' : mode === 'signIn' ? 'Sign in' : 'Create account'}
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => { setMode(mode === 'signIn' ? 'signUp' : 'signIn'); setError(null) }}
          >
            {mode === 'signIn' ? 'Create an account instead' : 'I already have an account'}
          </button>
        </div>
      </form>
    </div>
  )
}
