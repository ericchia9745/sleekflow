import { useState } from 'react'

import * as authApi from '../auth/api'
import { describeError } from '../api/todos'
import { useAuth } from '../auth/AuthContext'

type Mode = 'signIn' | 'signUp' | 'changePassword'

export function AuthScreen() {
  const { signIn, signUp } = useAuth()
  const [mode, setMode] = useState<Mode>('signIn')
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  function switchTo(next: Mode) {
    setMode(next)
    setError(null)
    setNotice(null)
    setPassword('')
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'signIn') {
        await signIn(username.trim(), password)
      } else if (mode === 'signUp') {
        await signUp(username.trim(), displayName, password)
      } else {
        await authApi.changePassword(username.trim(), password)
        switchTo('signIn')
        setNotice('Password changed. Sign in with your new password.')
      }
    } catch (err) {
      setError(describeError(err))
    } finally {
      setBusy(false)
    }
  }

  const title = { signIn: 'Sign in', signUp: 'Create an account', changePassword: 'Change password' }[mode]

  return (
    <div className="auth-screen">
      <form className="todo-form auth-form" onSubmit={submit}>
        <h2>{title}</h2>

        {notice && <p className="banner info" role="status">{notice}</p>}
        {error && <p className="error" role="alert">{error}</p>}

        {mode === 'changePassword' && (
          <p className="hint">
            No current password needed here -- if the username exists, this replaces its password outright.
          </p>
        )}

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
          {mode === 'changePassword' ? 'New password' : 'Password'}
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
            {busy
              ? 'Working…'
              : { signIn: 'Sign in', signUp: 'Create account', changePassword: 'Change password' }[mode]}
          </button>
          {mode === 'signIn' && (
            <button type="button" className="secondary" onClick={() => switchTo('signUp')}>
              Create an account instead
            </button>
          )}
          {mode === 'signUp' && (
            <button type="button" className="secondary" onClick={() => switchTo('signIn')}>
              I already have an account
            </button>
          )}
          {mode === 'changePassword' && (
            <button type="button" className="secondary" onClick={() => switchTo('signIn')}>
              Back to sign in
            </button>
          )}
        </div>

        {mode === 'signIn' && (
          <button type="button" className="link small" onClick={() => switchTo('changePassword')}>
            Forgot password?
          </button>
        )}
      </form>
    </div>
  )
}
