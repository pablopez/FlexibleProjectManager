import { afterEach, describe, expect, it, vi } from 'vitest'
import { getCurrentUser, login, logout, refreshSession } from './authApi'

describe('authApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('logs in with credentials and never expects a refresh token in JSON', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('{"accessToken":"access-token","expiresIn":900}', { status: 200 }),
    )
    await expect(login('admin@example.com', 'password123')).resolves.toEqual({ accessToken: 'access-token', expiresIn: 900 })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/login', expect.objectContaining({
      method: 'POST', credentials: 'include',
      body: JSON.stringify({ email: 'admin@example.com', password: 'password123' }),
    }))
  })

  it('restores from the HttpOnly cookie without a JSON body', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('{"accessToken":"new-token","expiresIn":900}', { status: 200 }),
    )
    await expect(refreshSession()).resolves.toMatchObject({ accessToken: 'new-token' })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/refresh', expect.objectContaining({ method: 'POST', credentials: 'include' }))
    expect((fetchMock.mock.calls[0][1] as RequestInit).body).toBeUndefined()
  })

  it('uses a bearer token for the current user and clears the server session on logout', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response('{"id":"1","email":"admin@example.com","displayName":"Admin","organization":{"id":"2","name":"Org"},"roles":["ADMIN"],"permissions":[]}', { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(getCurrentUser('access-token')).resolves.toMatchObject({ email: 'admin@example.com' })
    await expect(logout()).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ headers: { Accept: 'application/json', Authorization: 'Bearer access-token' } })
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST', credentials: 'include' })
  })
})
