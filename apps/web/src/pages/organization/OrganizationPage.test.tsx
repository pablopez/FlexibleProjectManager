import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OrganizationPage } from './OrganizationPage'
import { useAuth } from '../../shared/auth/AuthProvider'
import { getOrganization, updateOrganization } from '../../shared/api/organizationApi'
import { getInstallation, updateInstallation } from '../../shared/api/installationApi'

vi.mock('../../shared/auth/AuthProvider', () => ({ useAuth: vi.fn() }))
vi.mock('../../shared/api/organizationApi', () => ({ getOrganization: vi.fn(), updateOrganization: vi.fn() }))
vi.mock('../../shared/api/installationApi', () => ({ getInstallation: vi.fn(), updateInstallation: vi.fn() }))

const auth = vi.mocked(useAuth)
const orgApi = { get: vi.mocked(getOrganization), update: vi.mocked(updateOrganization) }
const installationApi = { get: vi.mocked(getInstallation), update: vi.mocked(updateInstallation) }
const organization = { id: 'org-1', name: 'Example Org', slug: 'example-org', status: 'ACTIVE' as const, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }
const installation = { id: 'install-1', organizationId: 'org-1', name: 'Local', platform: 'WINDOWS' as const, applicationVersion: '0.1.0', status: 'UNLICENSED' as const, createdAt: '2026-01-01T00:00:00Z', lastSeenAt: null }

describe('OrganizationPage', () => {
  afterEach(() => cleanup())
  beforeEach(() => {
    vi.clearAllMocks()
    auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['organization:read', 'organization:update'] } } as ReturnType<typeof useAuth>)
    orgApi.get.mockResolvedValue(organization); orgApi.update.mockResolvedValue({ ...organization, name: 'Updated' })
    installationApi.get.mockResolvedValue(installation); installationApi.update.mockResolvedValue({ ...installation, name: 'Updated Local' })
  })

  it('renders loading and organization/installation data', async () => {
    render(<OrganizationPage />)
    expect(await screen.findByDisplayValue('Example Org')).toBeTruthy()
    expect(screen.getByDisplayValue('Local')).toBeTruthy()
    expect(screen.getByText('WINDOWS')).toBeTruthy()
    expect(screen.getByText('install-1')).toBeTruthy()
  })

  it('renders loading while the initial requests are pending', async () => {
    let resolveOrganization!: (value: typeof organization) => void
    orgApi.get.mockReturnValueOnce(new Promise(resolve => { resolveOrganization = resolve }))
    render(<OrganizationPage />)
    expect(screen.getByText('Loading organization…')).toBeTruthy()
    resolveOrganization(organization)
    await screen.findByDisplayValue('Example Org')
  })

  it('renders an error state', async () => {
    orgApi.get.mockRejectedValueOnce(new Error('Unable to load organization.'))
    render(<OrganizationPage />)
    expect(await screen.findByText('Unable to load organization.')).toBeTruthy()
  })

  it('allows permitted edits and refreshes both resources', async () => {
    const user = userEvent.setup()
    orgApi.get.mockResolvedValueOnce(organization).mockResolvedValueOnce({ ...organization, name: 'Refreshed Org', slug: 'refreshed-org' })
    installationApi.get.mockResolvedValueOnce(installation).mockResolvedValueOnce({ ...installation, name: 'Refreshed Local' })
    render(<OrganizationPage />)
    await screen.findByDisplayValue('Example Org')
    const names = screen.getAllByLabelText('Name')
    await user.clear(names[0]); await user.type(names[0], 'Updated')
    await user.clear(names[1]); await user.type(names[1], 'Updated Local')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(orgApi.update).toHaveBeenCalledWith({ name: 'Updated', slug: 'example-org' })
    expect(installationApi.update).toHaveBeenCalledWith({ name: 'Updated Local' })
    await waitFor(() => expect(screen.getByDisplayValue('Refreshed Org')).toBeTruthy())
    expect(screen.getByDisplayValue('refreshed-org')).toBeTruthy()
    expect(screen.getByDisplayValue('Refreshed Local')).toBeTruthy()
  })

  it('renders a save error without pretending the update succeeded', async () => {
    const user = userEvent.setup()
    orgApi.update.mockRejectedValueOnce(new Error('Update failed.'))
    render(<OrganizationPage />)
    await screen.findByDisplayValue('Example Org')
    const name = screen.getAllByLabelText('Name')[0]
    await user.clear(name); await user.type(name, 'Failed update')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText('Update failed.')).toBeTruthy()
    expect(screen.queryByDisplayValue('Failed update')).toBeNull()
  })

  it('is read-only without organization:update', async () => {
    auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['organization:read'] } } as ReturnType<typeof useAuth>)
    render(<OrganizationPage />)
    expect((await screen.findByDisplayValue('Example Org')).getAttribute('readonly')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Save changes' })).toBeNull()
  })

  it('does not render sensitive technical values', async () => {
    render(<OrganizationPage />)
    await screen.findByDisplayValue('Example Org')
    expect(screen.queryByText(/database|private key|jwt/i)).toBeNull()
  })
})
