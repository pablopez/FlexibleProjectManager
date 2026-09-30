import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuditPage } from './AuditPage'
import { useAuth } from '../../shared/auth/AuthProvider'
import { listAudit } from '../../shared/api/auditApi'
import { getUserSettings } from '../../shared/api/userSettingsApi'
import { UserPreferencesProvider } from '../../shared/preferences/UserPreferencesProvider'

vi.mock('../../shared/auth/AuthProvider', () => ({ useAuth: vi.fn() }))
vi.mock('../../shared/api/auditApi', () => ({ listAudit: vi.fn() }))
vi.mock('../../shared/api/userSettingsApi', () => ({ getUserSettings: vi.fn(), updateUserSettings: vi.fn() }))

const auth = vi.mocked(useAuth)
const api = vi.mocked(listAudit)
function renderPage() { return render(<UserPreferencesProvider><AuditPage /></UserPreferencesProvider>) }
const row = { id: '1', actor: { id: 'u', displayName: 'Admin' }, action: 'PROJECT_CREATED', resourceType: 'PROJECT', resourceId: 'p', metadata: { changedFields: ['name'] }, createdAt: '2026-01-01T00:00:00Z' }

describe('AuditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks(); auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['audit:read'] } } as ReturnType<typeof useAuth>)
    vi.mocked(getUserSettings).mockResolvedValue({ language: 'en', theme: 'light' })
    api.mockResolvedValue({ items: [], page: 0, size: 25, total: 0, totalPages: 0 })
  })
  afterEach(() => cleanup())

  it('renders loading, empty and API error states', async () => {
    api.mockReturnValueOnce(new Promise(() => undefined)); renderPage(); expect(screen.getByText('Loading audit history…')).toBeTruthy(); cleanup()
    api.mockResolvedValueOnce({ items: [], page: 0, size: 25, total: 0, totalPages: 0 }); renderPage(); await screen.findByText('No audit entries found.')
    cleanup(); api.mockRejectedValueOnce(new Error('Audit unavailable')); renderPage(); await screen.findByText('Audit unavailable')
  })

  it('renders rows, translated labels, unknown fallback and preserves filters while paging', async () => {
    const user = userEvent.setup(); api.mockResolvedValueOnce({ items: [row, { ...row, id: '2', action: 'FUTURE_ACTION', metadata: null }], page: 0, size: 25, total: 26, totalPages: 2 }).mockResolvedValueOnce({ items: [row], page: 0, size: 25, total: 26, totalPages: 2 }).mockResolvedValue({ items: [], page: 1, size: 25, total: 26, totalPages: 2 })
    renderPage(); await screen.findAllByText('Admin'); expect(screen.getAllByText('Project created').length).toBeGreaterThan(0); expect(screen.getByText('name')).toBeTruthy(); expect(screen.getByText('FUTURE_ACTION')).toBeTruthy(); expect(screen.queryByRole('button', { name: /edit|delete/i })).toBeNull()
    await user.selectOptions(screen.getByRole('combobox', { name: 'Action' }), 'PROJECT_UPDATED'); await user.click(screen.getByRole('button', { name: 'Apply' })); await waitFor(() => expect(api).toHaveBeenLastCalledWith(0, 25, { action: 'PROJECT_UPDATED' }))
    await user.click(screen.getByRole('button', { name: 'Next' })); await waitFor(() => expect(api).toHaveBeenLastCalledWith(1, 25, { action: 'PROJECT_UPDATED' }))
  })

  it('uses Spanish action labels', async () => {
    vi.mocked(getUserSettings).mockResolvedValue({ language: 'es', theme: 'light' }); api.mockResolvedValue({ items: [row], page: 0, size: 25, total: 1, totalPages: 1 })
    renderPage(); await screen.findAllByText('Proyecto creado')
  })
})
