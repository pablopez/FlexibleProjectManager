import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { type InitializeSetupRequest } from '../../shared/api/setupApi'
import { useAuth } from '../../shared/auth/AuthProvider'

const emptySetup: InitializeSetupRequest = {
  organization: { name: '' },
  installation: { name: '' },
  administrator: { email: '', displayName: '', password: '' },
}

export function SetupPage() {
  const { initialize, error } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(emptySetup)
  const [submitting, setSubmitting] = useState(false)

  function update(section: keyof InitializeSetupRequest, field: string, value: string) {
    setForm((current) => ({ ...current, [section]: { ...current[section], [field]: value } }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await initialize(form)
      navigate('/login', { replace: true })
    } catch {
      // AuthProvider owns and exposes the user-facing setup error.
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="public-page">
      <section className="card">
        <p className="eyebrow">First-run setup</p>
        <h1>Initialize Flexible Project Manager</h1>
        <p className="muted">Create the first organization and administrator account.</p>
        {error && <p className="error" role="alert">{error}</p>}
        <form onSubmit={submit} className="stack-form" aria-label="First-run setup">
          <label>Organization name<input required maxLength={200} value={form.organization.name} onChange={(event) => update('organization', 'name', event.target.value)} /></label>
          <label>Installation name<input required maxLength={200} value={form.installation.name} onChange={(event) => update('installation', 'name', event.target.value)} /></label>
          <label>Administrator email<input required type="email" value={form.administrator.email} onChange={(event) => update('administrator', 'email', event.target.value)} /></label>
          <label>Administrator display name<input required maxLength={200} value={form.administrator.displayName} onChange={(event) => update('administrator', 'displayName', event.target.value)} /></label>
          <label>Administrator password<input required minLength={8} type="password" value={form.administrator.password} onChange={(event) => update('administrator', 'password', event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? 'Initializing…' : 'Initialize installation'}</button>
        </form>
      </section>
    </main>
  )
}
