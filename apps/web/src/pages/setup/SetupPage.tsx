import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { type InitializeSetupRequest } from '../../shared/api/setupApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

const emptySetup: InitializeSetupRequest = {
  organization: { name: '' },
  installation: { name: '' },
  administrator: { email: '', displayName: '', password: '' },
}

export function SetupPage() {
  const { initialize, error } = useAuth()
  const { t } = useUserPreferences()
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
        <p className="eyebrow">{t('setup.eyebrow')}</p>
        <h1>{t('setup.title')}</h1>
        <p className="muted">{t('setup.description')}</p>
        {error && <p className="error" role="alert">{error === 'Unable to connect to Flexible Project Manager.' ? t('common.connectionError') : error === 'Setup failed.' ? t('setup.failed') : error}</p>}
        <form onSubmit={submit} className="stack-form" aria-label="First-run setup">
          <label>{t('setup.organization')}<input required maxLength={200} value={form.organization.name} onChange={(event) => update('organization', 'name', event.target.value)} /></label>
          <label>{t('setup.installation')}<input required maxLength={200} value={form.installation.name} onChange={(event) => update('installation', 'name', event.target.value)} /></label>
          <label>{t('setup.adminEmail')}<input required type="email" value={form.administrator.email} onChange={(event) => update('administrator', 'email', event.target.value)} /></label>
          <label>{t('setup.adminDisplayName')}<input required maxLength={200} value={form.administrator.displayName} onChange={(event) => update('administrator', 'displayName', event.target.value)} /></label>
          <label>{t('setup.adminPassword')}<input required minLength={8} type="password" value={form.administrator.password} onChange={(event) => update('administrator', 'password', event.target.value)} /></label>
          <button type="submit" disabled={submitting}>{submitting ? t('setup.initializing') : t('setup.initialize')}</button>
        </form>
      </section>
    </main>
  )
}
