import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearAccessToken, setAccessToken } from '../auth/authSession'
import { createUser, listUsers, listRoles, replaceUserRoles, updateUser } from './usersApi'

describe('usersApi', () => {
  afterEach(() => { clearAccessToken(); vi.restoreAllMocks() })

  it('lists users with pagination and status filter', async () => {
    setAccessToken('token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"items":[]}', { status: 200 }))
    await listUsers(1, 25, 'DISABLED')
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/users?page=1&size=25&status=DISABLED', expect.anything())
  })

  it('sends only the user creation fields and supports role replacement', async () => {
    setAccessToken('token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"id":"u"}', { status: 200 }))
    await createUser({ email: 'user@example.com', displayName: 'User', password: 'password123', roles: ['USER'] })
    await updateUser('u', { displayName: 'Updated' })
    await replaceUserRoles('u', ['VIEWER'])
    await listRoles()
    expect(fetchMock.mock.calls[0][1]).toEqual(expect.objectContaining({ method: 'POST', body: '{"email":"user@example.com","displayName":"User","password":"password123","roles":["USER"]}' }))
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({ method: 'PATCH', body: '{"displayName":"Updated"}' }))
    expect(fetchMock.mock.calls[2][1]).toEqual(expect.objectContaining({ method: 'PUT', body: '{"roles":["VIEWER"]}' }))
  })
})
