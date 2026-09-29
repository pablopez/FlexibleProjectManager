import { useEffect, useState } from 'react'
import { getInstallation, type Installation } from '../../shared/api/installationApi'
import { activateLicense, deactivateLicense, getEntitlements, getLicense, isLicenseNotFound, type Entitlements, type License } from '../../shared/api/licenseApi'
import { useAuth } from '../../shared/auth/AuthProvider'

function format(value: string | null) { return value ? new Date(value).toLocaleString() : '—' }

export function LicensePage() {
  const { user } = useAuth()
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
      setError(cause instanceof Error ? cause.message : 'Unable to load license information.')
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
      setError(cause instanceof Error ? cause.message : 'Unable to activate the license.')
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
      setError(cause instanceof Error ? cause.message : 'Unable to deactivate the license.')
    } finally {
      setSaving(false)
    }
  }

  if (!canRead) return <section className="page-content"><h1>License</h1><p className="alert alert-error">You do not have permission to view licensing.</p></section>

  return <section className="page-content">
    <div className="page-heading"><div><p className="eyebrow">Platform Core</p><h1>License</h1><p className="muted">Manage the signed license for this installation.</p></div></div>
    {loading && <p className="muted" aria-live="polite">Loading license…</p>}
    {error && <p className="alert alert-error">{error}</p>}
    {!loading && installation && <>
      <div className="card">
        <h2>License status</h2>
        <dl>
          <dt>Status</dt><dd>{license?.status ?? 'UNLICENSED'}</dd>
          <dt>Installation ID</dt><dd>{installation.id}</dd>
          {license?.licenseId && <><dt>License ID</dt><dd>{license.licenseId}</dd></>}
          {license?.type && <><dt>Type</dt><dd>{license.type}</dd></>}
          {license && <><dt>Issued</dt><dd>{format(license.issuedAt)}</dd><dt>Expires</dt><dd>{license.expiresAt ? format(license.expiresAt) : 'Perpetual'}</dd></>}
          <dt>Max users</dt><dd>{entitlements.maxUsers ?? '—'}</dd>
        </dl>
        <h3>License features</h3>
        {entitlements.licenseFeatures.length === 0 ? <p className="muted">None</p> : <ul>{entitlements.licenseFeatures.map(feature => <li key={feature}>{feature}</li>)}</ul>}
      </div>
      {canManage && <div className="card">
        <h2>Activate or replace</h2>
        <label>Signed license<textarea value={signedLicense} onChange={event => setSignedLicense(event.target.value)} rows={6} required /></label>
        <button type="button" disabled={saving || !signedLicense.trim()} onClick={() => void activate()}>{saving ? 'Saving…' : 'Activate license'}</button>
        {license && <button type="button" disabled={saving} onClick={() => void deactivate()}>Deactivate license</button>}
      </div>}
    </>}
  </section>
}
