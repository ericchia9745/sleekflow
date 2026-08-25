import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthScreen } from '../components/AuthScreen'
import * as authApi from '../auth/api'

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ signIn: vi.fn(), signUp: vi.fn() }),
}))

vi.mock('../auth/api')

describe('AuthScreen', () => {
  beforeEach(() => {
    vi.mocked(authApi.changePassword).mockReset()
  })

  it('resets a password by username alone, with no current password asked for', async () => {
    vi.mocked(authApi.changePassword).mockResolvedValue(undefined)
    render(<AuthScreen />)

    fireEvent.click(screen.getByRole('button', { name: 'Forgot password?' }))

    // The point of this flow: nothing here asks for the current password.
    expect(screen.queryByLabelText('Password')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'eric' } })
    fireEvent.change(screen.getByLabelText('New password'), { target: { value: 'a new password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Change password' }))

    await waitFor(() =>
      expect(authApi.changePassword).toHaveBeenCalledWith('eric', 'a new password'),
    )
    // Success lands back on the sign-in form rather than leaving the reset form up.
    expect(await screen.findByText(/Password changed/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('surfaces the server error when the username does not exist', async () => {
    vi.mocked(authApi.changePassword).mockRejectedValue({
      response: { data: { detail: "No account with username 'nobody'." } },
    })
    render(<AuthScreen />)

    fireEvent.click(screen.getByRole('button', { name: 'Forgot password?' }))
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'nobody' } })
    fireEvent.change(screen.getByLabelText('New password'), { target: { value: 'a new password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Change password' }))

    expect(await screen.findByRole('alert')).toHaveTextContent("No account with username 'nobody'.")
    // Stays on the reset form so the user can correct the username.
    expect(screen.getByRole('heading', { name: 'Change password' })).toBeInTheDocument()
  })

  it('returns to sign-in without submitting anything', () => {
    render(<AuthScreen />)

    fireEvent.click(screen.getByRole('button', { name: 'Forgot password?' }))
    fireEvent.click(screen.getByRole('button', { name: 'Back to sign in' }))

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(authApi.changePassword).not.toHaveBeenCalled()
  })
})
