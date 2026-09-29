import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LicensePage } from './LicensePage'
import { useAuth } from '../../shared/auth/AuthProvider'
import { getInstallation } from '../../shared/api/installationApi'
import { activateLicense, deactivateLicense, getEntitlements, getLicense, isLicenseNotFound } from '../../shared/api/licenseApi'

vi.mock('../../shared/auth/AuthProvider', () => ({ useAuth: vi.fn() }))
vi.mock('../../shared/api/installationApi', () => ({ getInstallation: vi.fn() }))
vi.mock('../../shared/api/licenseApi', () => ({
  activateLicense: vi.fn(), deactivateLicense: vi.fn(), getEntitlements: vi.fn(), getLicense: vi.fn(), isLicenseNotFound: vi.fn(),
}))

const auth = vi.mocked(useAuth)
const installationApi = vi.mocked(getInstallation)
const licenseApi = {
  activate: vi.mocked(activateLicense), deactivate: vi.mocked(deactivateLicense), entitlements: vi.mocked(getEntitlements),
  get: vi.mocked(getLicense), notFound: vi.mocked(isLicenseNotFound),
}
const installation = { id: 'install-1', organizationId: 'org-1', name: 'Local', platform: 'WINDOWS' as const, applicationVersion: '0.1.0', status: 'UNLICENSED' as const, createdAt: '2026-01-01T00:00:00Z', lastSeenAt: null }
const active = { licenseId: 'license-1', organizationId: 'org-1', installationId: 'install-1', status: 'ACTIVE' as const, type: 'SUBSCRIPTION' as const, issuedAt: '2026-01-01T00:00:00Z', expiresAt: '2027-01-01T00:00:00Z', licenseFeatures: ['module.video-qc', 'video-qc.hdr'], limits: { maxUsers: 25 }, signatureAlgorithm: 'Ed25519' as const }

describe('LicensePage', () => {
  afterEach(() => cleanup())
  beforeEach(() => {
    vi.clearAllMocks()
    auth.mockReturnValue({ status: 'authenticated', user: { roles: ['ADMIN'], permissions: ['license:read', 'license:manage'] } } as ReturnType<typeof useAuth>)
    installationApi.mockResolvedValue(installation)
    licenseApi.entitlements.mockResolvedValue({ maxUsers: 25, licenseFeatures: active.licenseFeatures })
    licenseApi.get.mockResolvedValue(active)
    licenseApi.activate.mockResolvedValue(active)
    licenseApi.deactivate.mockResolvedValue(undefined)
    licenseApi.notFound.mockReturnValue(false)
  })

  it('renders loading and active license details', async () => {
    render(<LicensePage />)
    expect(screen.getByText('Loading license…')).toBeTruthy()
    expect(await screen.findByText('ACTIVE')).toBeTruthy()
    expect(screen.getByText('install-1')).toBeTruthy()
    expect(screen.getByText('module.video-qc')).toBeTruthy()
    expect(screen.getByText('25')).toBeTruthy()
  })

  it('renders the unlicensed state and activation form from a 404', async () => {
    licenseApi.get.mockRejectedValueOnce(new Error('not found'))
    licenseApi.notFound.mockReturnValue(true)
    render(<LicensePage />)
    expect(await screen.findByText('UNLICENSED')).toBeTruthy()
    expect(screen.getByText('install-1')).toBeTruthy()
    expect(screen.getByLabelText('Signed license')).toBeTruthy()
  })

  it('activates and refreshes the rendered license', async () => {
    const user = userEvent.setup()
    const updated = { ...active, licenseId: 'license-2', licenseFeatures: ['module.text-editor'] }
    licenseApi.get.mockResolvedValueOnce(active).mockResolvedValueOnce(updated)
    licenseApi.entitlements.mockResolvedValueOnce({ maxUsers: 25, licenseFeatures: active.licenseFeatures }).mockResolvedValueOnce({ maxUsers: 10, licenseFeatures: updated.licenseFeatures })
    render(<LicensePage />)
    await screen.findByText('ACTIVE')
    await user.type(screen.getByLabelText('Signed license'), 'signed-jws')
    await user.click(screen.getByRole('button', { name: 'Activate license' }))
    expect(licenseApi.activate).toHaveBeenCalledWith('signed-jws')
    expect(await screen.findByText('module.text-editor')).toBeTruthy()
    expect(screen.getByText('license-2')).toBeTruthy()
  })

  it('is read-only without license:manage', async () => {
    auth.mockReturnValue({ status: 'authenticated', user: { roles: ['USER'], permissions: ['license:read'] } } as ReturnType<typeof useAuth>)
    render(<LicensePage />)
    await screen.findByText('ACTIVE')
    expect(screen.queryByRole('button', { name: 'Activate license' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Deactivate license' })).toBeNull()
  })

  it('renders an expired license without effective entitlements', async () => {
    licenseApi.get.mockResolvedValue({ ...active, status: 'EXPIRED', licenseFeatures: ['module.video-qc'] })
    licenseApi.entitlements.mockResolvedValue({ maxUsers: null, licenseFeatures: [] })
    render(<LicensePage />)
    expect(await screen.findByText('EXPIRED')).toBeTruthy()
    expect(screen.getByText('—')).toBeTruthy()
    expect(screen.getByText('None')).toBeTruthy()
  })

  it('renders an invalid license without claim-derived fields', async () => {
    licenseApi.get.mockResolvedValue({ ...active, status: 'INVALID', licenseId: null, type: null, issuedAt: null, expiresAt: null, licenseFeatures: null, limits: null, signatureAlgorithm: null })
    licenseApi.entitlements.mockResolvedValue({ maxUsers: null, licenseFeatures: [] })
    render(<LicensePage />)
    expect(await screen.findByText('INVALID')).toBeTruthy()
    expect(screen.queryByText('license-1')).toBeNull()
    expect(screen.getByText('None')).toBeTruthy()
  })

  it('displays activation failures', async () => {
    const user = userEvent.setup()
    licenseApi.activate.mockRejectedValue(new Error('The license could not be used.'))
    render(<LicensePage />)
    await screen.findByText('ACTIVE')
    await user.type(screen.getByLabelText('Signed license'), 'bad-license')
    await user.click(screen.getByRole('button', { name: 'Activate license' }))
    expect(await screen.findByText('The license could not be used.')).toBeTruthy()
  })

  it('displays a generic loading error', async () => {
    installationApi.mockRejectedValue(new Error('Network unavailable'))
    render(<LicensePage />)
    expect(await screen.findByText('Network unavailable')).toBeTruthy()
  })

  it('deactivates and refreshes to the unlicensed state', async () => {
    const user = userEvent.setup()
    licenseApi.get.mockResolvedValueOnce(active).mockRejectedValueOnce(new Error('not found'))
    licenseApi.notFound.mockReturnValue(true)
    licenseApi.entitlements.mockResolvedValueOnce({ maxUsers: 25, licenseFeatures: active.licenseFeatures }).mockResolvedValueOnce({ maxUsers: null, licenseFeatures: [] })
    render(<LicensePage />)
    await screen.findByText('ACTIVE')
    await user.click(screen.getByRole('button', { name: 'Deactivate license' }))
    expect(licenseApi.deactivate).toHaveBeenCalledOnce()
    expect(await screen.findByText('UNLICENSED')).toBeTruthy()
  })
})
