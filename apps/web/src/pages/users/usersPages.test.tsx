import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { UsersPage } from './UsersPage'
import { CreateUserPage } from './CreateUserPage'
import { UserDetailPage } from './UserDetailPage'
import { useAuth } from '../../shared/auth/AuthProvider'
import { createUser, getUser, listRoles, listUsers, replaceUserRoles, updateUser } from '../../shared/api/usersApi'

vi.mock('../../shared/auth/AuthProvider', () => ({ useAuth: vi.fn() }))
vi.mock('../../shared/api/usersApi', () => ({
  createUser: vi.fn(), getUser: vi.fn(), listRoles: vi.fn(), listUsers: vi.fn(),
  replaceUserRoles: vi.fn(), updateUser: vi.fn(),
}))

const auth = vi.mocked(useAuth)
const api = { createUser: vi.mocked(createUser), getUser: vi.mocked(getUser), listRoles: vi.mocked(listRoles), listUsers: vi.mocked(listUsers), replaceUserRoles: vi.mocked(replaceUserRoles), updateUser: vi.mocked(updateUser) }
const roles = [{ code: 'USER', name: 'USER', description: null, permissions: ['projects:read'] }, { code: 'ADMIN', name: 'ADMIN', description: null, permissions: ['users:read', 'users:update'] }]
const user = { id: 'u1', email: 'user@example.com', displayName: 'User', status: 'ACTIVE' as const, roles: ['USER'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }

function renderDetail(permissions = ['users:read', 'users:update']) {
  return render(<MemoryRouter initialEntries={['/app/users/u1']}><Routes><Route path="/app/users/:userId" element={<UserDetailPage />} /></Routes></MemoryRouter>)
}

describe('user pages', () => {
  afterEach(() => cleanup())

  beforeEach(() => {
    vi.clearAllMocks()
    auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['users:read', 'users:create', 'users:update'] } } as ReturnType<typeof useAuth>)
    api.listUsers.mockResolvedValue({ items: [user], page: 0, size: 25, total: 1, totalPages: 1 })
    api.listRoles.mockResolvedValue({ items: roles })
    api.getUser.mockResolvedValue(user)
    api.createUser.mockResolvedValue(user)
    api.updateUser.mockResolvedValue(user)
    api.replaceUserRoles.mockResolvedValue(user)
  })

  it('loads, renders, filters, and shows the create action by permission', async () => {
    render(<MemoryRouter><UsersPage /></MemoryRouter>)
    expect(await screen.findByText('User')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'New user' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Disabled' }))
    await waitFor(() => expect(api.listUsers).toHaveBeenLastCalledWith(0, 25, 'DISABLED'))
  })

  it('renders empty and error states', async () => {
    api.listUsers.mockResolvedValueOnce({ items: [], page: 0, size: 25, total: 0, totalPages: 0 })
    const view = render(<MemoryRouter><UsersPage /></MemoryRouter>)
    expect(await screen.findByText('No users found.')).toBeTruthy()
    view.unmount()
    api.listUsers.mockRejectedValueOnce(new Error('Unable to load users.'))
    render(<MemoryRouter><UsersPage /></MemoryRouter>)
    expect(await screen.findByText('Unable to load users.')).toBeTruthy()
  })

  it('loads roles and sends only create fields', async () => {
    const userEventApi = userEvent.setup()
    render(<MemoryRouter><CreateUserPage /></MemoryRouter>)
    await screen.findByText('USER')
    await userEventApi.type(screen.getByLabelText('Email'), 'new@example.com')
    await userEventApi.type(screen.getByLabelText('Display name'), 'New User')
    await userEventApi.type(screen.getByLabelText('Password'), 'password123')
    await userEventApi.click(screen.getByLabelText(/USER/))
    await userEventApi.click(screen.getByRole('button', { name: 'Create user' }))
    expect(api.createUser).toHaveBeenCalledWith({ email: 'new@example.com', displayName: 'New User', password: 'password123', roles: ['USER'] })
  })

  it('hides the create action without users:create', async () => {
    auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['users:read'] } } as ReturnType<typeof useAuth>)
    render(<MemoryRouter><UsersPage /></MemoryRouter>)
    await screen.findByText('User')
    expect(screen.queryByRole('link', { name: 'New user' })).toBeNull()
  })

  it('renders detail controls and displays the last-admin server error', async () => {
    const userEventApi = userEvent.setup()
    renderDetail()
    expect((await screen.findByDisplayValue('user@example.com')).getAttribute('readonly')).not.toBeNull()
    expect(screen.getByRole('button', { name: 'Disable' })).toBeTruthy()
    api.updateUser.mockRejectedValueOnce(new Error('The organization must retain an active administrator.'))
    await userEventApi.click(screen.getByRole('button', { name: 'Disable' }))
    expect(await screen.findByText('The organization must retain an active administrator.')).toBeTruthy()
  })

  it('makes the detail page read-only without users:update', async () => {
    auth.mockReturnValue({ status: 'authenticated', user: { permissions: ['users:read'] } } as ReturnType<typeof useAuth>)
    renderDetail(['users:read'])
    await screen.findByDisplayValue('user@example.com')
    expect(screen.queryByRole('button', { name: 'Disable' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Save roles' })).toBeNull()
  })
})
