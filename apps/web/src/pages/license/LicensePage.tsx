import { useEffect, useState } from 'react'
import { getInstallation, type Installation } from '../../shared/api/installationApi'
import { activateLicense, deactivateLicense, getEntitlements, getLicense, isLicenseNotFound, type Entitlements, type License } from '../../shared/api/licenseApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

function format(value: string | null) { return value ? new Date(value).toLocaleString() : '—' }

export function LicensePage() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  const permissions = user?.permissions ?? []
  const canRead = permissions.includes('license:read')
  const canManage = permissions.includes('license:manage')
  const [license, setLicense] = useState<License | null>(null)
  const [installation, setInstallation] = useState<Installation | null>(null)
  const [entitlements, setEntitlements] = useState<Entitlements>({ maxUsers: null, licenseFeatures: [] })
  const [signedLicense, setSignedLicense] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [localInstallation, effectiveEntitlements] = await Promise.all([getInstallation(), getEntitlements()])
      let currentLicense: License | null = null
      try {
        currentLicense = await getLicense()
      } catch (cause) {
        if (!isLicenseNotFound(cause)) throw cause
      }
      setInstallation(localInstallation)
      setEntitlements(effectiveEntitlements)
      setLicense(currentLicense)
     } catch (cause) {
       setError(cause instanceof Error ? cause.message : t('license.loadError'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (canRead) void load()
  }, [canRead])

  async function activate() {
    setSaving(true)
    setError(null)
    try {
      await activateLicense(signedLicense)
      setSignedLicense('')
      await load()
     } catch (cause) {
       setError(cause instanceof Error ? cause.message : t('license.activateError'))
    } finally {
      setSaving(false)
    }
  }

  async function deactivate() {
    setSaving(true)
    setError(null)
    try {
      await deactivateLicense()
      await load()
     } catch (cause) {
       setError(cause instanceof Error ? cause.message : t('license.deactivateError'))
    } finally {
      setSaving(false)
    }
  }

  if (!canRead) return <section className="page-content"><h1>{t('license.title')}</h1><p className="alert alert-error">{t('license.permission')}</p></section>

  return <section className="page-content">
     <div className="page-heading"><div><p className="eyebrow">{t('license.eyebrow')}</p><h1>{t('license.title')}</h1><p className="muted">{t('license.description')}</p></div></div>
     {loading && <p className="muted" aria-live="polite">{t('license.loading')}</p>}
    {error && <p className="alert alert-error">{error}</p>}
    {!loading && installation && <>
      <div className="card">
         <h2>{t('license.status')}</h2>
        <dl>
           <dt>{t('users.status')}</dt><dd>{license?.status ?? t('license.unlicensed')}</dd>
           <dt>{t('license.installationId')}</dt><dd>{installation.id}</dd>
           {license?.licenseId && <><dt>{t('license.licenseId')}</dt><dd>{license.licenseId}</dd></>}
           {license?.type && <><dt>{t('license.type')}</dt><dd>{license.type}</dd></>}
           {license && <><dt>{t('license.issued')}</dt><dd>{format(license.issuedAt)}</dd><dt>{t('license.expires')}</dt><dd>{license.expiresAt ? format(license.expiresAt) : t('license.perpetual')}</dd></>}
           <dt>{t('license.maxUsers')}</dt><dd>{entitlements.maxUsers ?? '—'}</dd>
        </dl>
         <h3>{t('license.features')}</h3>
         {entitlements.licenseFeatures.length === 0 ? <p className="muted">{t('common.none')}</p> : <ul>{entitlements.licenseFeatures.map(feature => <li key={feature}>{feature}</li>)}</ul>}
      </div>
      {canManage && <div className="card">
         <h2>{t('license.activateOrReplace')}</h2>
         <label>{t('license.signed')}<textarea value={signedLicense} onChange={event => setSignedLicense(event.target.value)} rows={6} required /></label>
         <button type="button" disabled={saving || !signedLicense.trim()} onClick={() => void activate()}>{saving ? t('common.saving') : t('license.activate')}</button>
         {license && <button type="button" disabled={saving} onClick={() => void deactivate()}>{t('license.deactivate')}</button>}
      </div>}
    </>}
  </section>
}
