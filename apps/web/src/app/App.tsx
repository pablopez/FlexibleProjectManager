import { useEffect, useState, type FormEvent } from 'react'
import { getSetupStatus, initializeSetup, type InitializeSetupRequest } from '../shared/api/setupApi'
import { getCurrentUser, login, logout, refreshSession, type CurrentUserResponse } from '../shared/api/authApi'
import { clearAccessToken, setAccessToken } from '../shared/auth/authSession'

type AppState =
  | { kind: 'loading' }
  | { kind: 'setup' }
  | { kind: 'login' }
  | { kind: 'authenticated'; user: CurrentUserResponse }
  | { kind: 'error'; message: string }

type SetupForm = InitializeSetupRequest

const emptySetup: SetupForm = {
  organization: { name: '' },
  installation: { name: '' },
  administrator: { email: '', displayName: '', password: '' },
}

export function App() {
  const [state, setState] = useState<AppState>({ kind: 'loading' })
  const [setupForm, setSetupForm] = useState<SetupForm>(emptySetup)
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getSetupStatus()
      .then(async (setup) => {
        if (!setup.initialized) {
          setState({ kind: 'setup' })
          return
        }
        try {
          const session = await refreshSession()
          setAccessToken(session.accessToken)
          setState({ kind: 'authenticated', user: await getCurrentUser(session.accessToken) })
        } catch {
          clearAccessToken()
          setState({ kind: 'login' })
        }
      })
      .catch((error: unknown) => setState({ kind: 'error', message: error instanceof Error ? error.message : 'Unable to reach the backend.' }))
  }, [])

  function updateSetup(section: keyof SetupForm, field: string, value: string) {
    setSetupForm((current) => ({ ...current, [section]: { ...current[section], [field]: value } }))
  }

  async function submitSetup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await initializeSetup(setupForm)
      setState({ kind: 'login' })
    } catch (error: unknown) {
      setState({ kind: 'error', message: error instanceof Error ? error.message : 'Setup failed.' })
    } finally {
      setSubmitting(false)
    }
  }

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      const session = await login(loginEmail, loginPassword)
      setAccessToken(session.accessToken)
      setState({ kind: 'authenticated', user: await getCurrentUser(session.accessToken) })
    } catch (error: unknown) {
      clearAccessToken()
      setState({ kind: 'error', message: error instanceof Error ? error.message : 'Authentication failed.' })
    } finally {
      setSubmitting(false)
    }
  }

  async function signOut() {
    try {
      await logout()
    } finally {
      clearAccessToken()
      setState({ kind: 'login' })
    }
  }

  return (
    <main>
      <h1>Flexible Project Manager</h1>
      {state.kind === 'loading' && <p>Checking session…</p>}
      {state.kind === 'error' && <p role="alert">{state.message}</p>}
      {state.kind === 'setup' && (
        <form onSubmit={submitSetup} aria-label="First-run setup">
          <h2>Initialize your installation</h2>
          <label>Organization name<input required maxLength={200} value={setupForm.organization.name} onChange={(event) => updateSetup('organization', 'name', event.target.value)} /></label>
          <label>Installation name<input required maxLength={200} value={setupForm.installation.name} onChange={(event) => updateSetup('installation', 'name', event.target.value)} /></label>
          <label>Administrator email<input required type="email" value={setupForm.administrator.email} onChange={(event) => updateSetup('administrator', 'email', event.target.value)} /></label>
          <label>Administrator display name<input required maxLength={200} value={setupForm.administrator.displayName} onChange={(event) => updateSetup('administrator', 'displayName', event.target.value)} /></label>
          <label>Administrator password<input required minLength={8} type="password" value={setupForm.administrator.password} onChange={(event) => updateSetup('administrator', 'password', event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? 'Initializing…' : 'Initialize installation'}</button>
        </form>
      )}
      {state.kind === 'login' && (
        <form onSubmit={submitLogin} aria-label="Login">
          <h2>Sign in</h2>
          <label>Email<input required type="email" value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} /></label>
          <label>Password<input required type="password" value={loginPassword} onChange={(event) => setLoginPassword(event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'}</button>
        </form>
      )}
      {state.kind === 'authenticated' && (
        <section aria-label="Authenticated state">
          <p>Signed in as:</p>
          <strong>{state.user.displayName}</strong>
          <p>{state.user.email}</p>
          <button type="button" onClick={signOut}>Logout</button>
        </section>
      )}
    </main>
  )
}
