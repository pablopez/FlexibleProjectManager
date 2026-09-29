import { useState, type FormEvent } from 'react'
import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function LoginPage() {
  const { login, error } = useAuth()
  const { t } = useUserPreferences()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await login(email, password)
    } catch {
      // AuthProvider owns and exposes the user-facing authentication error.
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="public-page">
      <section className="card">
        <p className="eyebrow">Flexible Project Manager</p>
        <h1>{t('auth.signIn')}</h1>
        <p className="muted">{t('auth.continue')}</p>
        {error && <p className="error" role="alert">{error === 'Unable to connect to Flexible Project Manager.' ? t('common.connectionError') : error === 'Authentication failed.' ? t('auth.failed') : error}</p>}
        <form onSubmit={submit} className="stack-form" aria-label="Login">
          <label>{t('auth.email')}<input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label>{t('auth.password')}<input required type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? t('auth.signingIn') : t('auth.signIn')}</button>
        </form>
      </section>
    </main>
  )
}
