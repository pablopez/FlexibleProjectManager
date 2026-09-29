import { useEffect, useState, type FormEvent } from 'react'
import { getInstallation, updateInstallation, type Installation } from '../../shared/api/installationApi'
import { getOrganization, updateOrganization, type Organization } from '../../shared/api/organizationApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

function can(permissions: string[], permission: string) { return permissions.includes(permission) }
function format(value: string | null) { return value ? new Date(value).toLocaleString() : '—' }

export function OrganizationPage() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  const permissions = user?.permissions ?? []
  const canRead = can(permissions, 'organization:read')
  const canUpdate = can(permissions, 'organization:update')
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [installation, setInstallation] = useState<Installation | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [installationName, setInstallationName] = useState('')

  useEffect(() => {
    if (!canRead) { setLoading(false); return }
    let active = true
    setLoading(true); setError(null)
    Promise.all([getOrganization(), getInstallation()]).then(([org, localInstallation]) => {
      if (!active) return
      setOrganization(org); setName(org.name); setSlug(org.slug ?? '')
      setInstallation(localInstallation); setInstallationName(localInstallation.name)
    }).catch(cause => { if (active) setError(cause instanceof Error ? cause.message : t('organization.loadError')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [canRead])

  if (!canRead) return <section className="page-content"><h1>{t('common.forbidden')}</h1><p className="alert alert-error">{t('organization.permission')}</p></section>

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(null)
    try {
      const [org, localInstallation] = await Promise.all([
        updateOrganization({ name, slug: slug || null }),
        updateInstallation({ name: installationName }),
      ])
      const [refreshedOrganization, refreshedInstallation] = await Promise.all([getOrganization(), getInstallation()])
      setOrganization(refreshedOrganization); setName(refreshedOrganization.name); setSlug(refreshedOrganization.slug ?? '')
      setInstallation(refreshedInstallation); setInstallationName(refreshedInstallation.name)
    } catch (cause) { setError(cause instanceof Error ? cause.message : t('organization.saveError')) }
    finally { setSaving(false) }
  }

  return <section className="page-content">
     <div className="page-heading"><div><p className="eyebrow">{t('organization.eyebrow')}</p><h1>{t('organization.title')}</h1><p className="muted">{t('organization.description')}</p></div></div>
     {loading && <p className="muted" aria-live="polite">{t('organization.loading')}</p>}
    {error && <p className="alert alert-error">{error}</p>}
    {!loading && !error && organization && installation && <form className="stack-form" onSubmit={save}>
       <div className="card"><h2>{t('organization.organization')}</h2>
         <label>{t('organization.name')}<input required maxLength={200} readOnly={!canUpdate} value={name} onChange={event => setName(event.target.value)} /></label>
         <label>{t('organization.slug')}<input maxLength={100} readOnly={!canUpdate} value={slug} onChange={event => setSlug(event.target.value)} /></label>
         <dl><dt>{t('organization.identifier')}</dt><dd>{organization.id}</dd><dt>{t('users.status')}</dt><dd>{organization.status}</dd><dt>{t('organization.created')}</dt><dd>{format(organization.createdAt)}</dd><dt>{t('organization.updated')}</dt><dd>{format(organization.updatedAt)}</dd></dl>
      </div>
       <div className="card"><h2>{t('organization.installation')}</h2>
        <label>{t('organization.name')}<input required maxLength={200} readOnly={!canUpdate} value={installationName} onChange={event => setInstallationName(event.target.value)} /></label>
         <dl><dt>{t('organization.identifier')}</dt><dd>{installation.id}</dd><dt>{t('organization.platform')}</dt><dd>{installation.platform}</dd><dt>{t('organization.version')}</dt><dd>{installation.applicationVersion}</dd><dt>{t('users.status')}</dt><dd>{installation.status}</dd><dt>{t('organization.created')}</dt><dd>{format(installation.createdAt)}</dd><dt>{t('organization.lastSeen')}</dt><dd>{format(installation.lastSeenAt)}</dd></dl>
      </div>
       {canUpdate && <button type="submit" disabled={saving}>{saving ? t('organization.saving') : t('organization.save')}</button>}
    </form>}
  </section>
}
