import { getAccessToken } from '../auth/authSession'
import { request, requestJson, ApiError } from './apiError'

export type LicenseStatus = 'ACTIVE' | 'EXPIRED' | 'INVALID'
export type LicenseType = 'TRIAL' | 'SUBSCRIPTION' | 'PERPETUAL' | 'DEVELOPMENT'
export type License = {
  licenseId: string | null
  organizationId: string
  installationId: string
  status: LicenseStatus
  type: LicenseType | null
  issuedAt: string | null
  expiresAt: string | null
  licenseFeatures: string[] | null
  limits: { maxUsers: number } | null
  signatureAlgorithm: 'Ed25519' | null
}
export type Entitlements = { maxUsers: number | null; licenseFeatures: string[] }

const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
function headers() { return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` } }
function jsonHeaders() { return { ...headers(), 'Content-Type': 'application/json' } }

export function getLicense(): Promise<License> {
  return requestJson(`${base}/license`, { headers: headers() }, 'License request')
}

export function activateLicense(signedLicense: string): Promise<License> {
  return requestJson(`${base}/license/activate`, {
    method: 'POST', headers: jsonHeaders(), body: JSON.stringify({ signedLicense }),
  }, 'License activation request')
}

export function deactivateLicense(): Promise<void> {
  return request(`${base}/license/deactivate`, { method: 'POST', headers: headers() }, 'License deactivation request').then(() => undefined)
}

export function getEntitlements(): Promise<Entitlements> {
  return requestJson(`${base}/license/entitlements`, { headers: headers() }, 'License entitlements request')
}

export function isLicenseNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.kind === 'http' && error.status === 404
}
