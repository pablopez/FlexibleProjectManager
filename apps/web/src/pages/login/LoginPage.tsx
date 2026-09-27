import { useState, type FormEvent } from 'react'
import { useAuth } from '../../shared/auth/AuthProvider'

export function LoginPage() {
  const { login, error } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await login(email, password)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="public-page">
      <section className="card">
        <p className="eyebrow">Flexible Project Manager</p>
        <h1>Sign in</h1>
        <p className="muted">Use your platform account to continue.</p>
        {error && <p className="error" role="alert">{error}</p>}
        <form onSubmit={submit} className="stack-form" aria-label="Login">
          <label>Email<input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label>Password<input required type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'}</button>
        </form>
      </section>
    </main>
  )
}
