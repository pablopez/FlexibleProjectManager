import { afterEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken, clearAccessToken } from '../auth/authSession'
import { createProject, listProjects } from './projectsApi'

describe('projectsApi', () => {
  afterEach(() => { clearAccessToken(); vi.restoreAllMocks() })

  it('lists projects with the in-memory bearer token', async () => {
    setAccessToken('access-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"items":[],"page":0,"size":20,"total":0,"totalPages":0}', { status: 200 }))
    await expect(listProjects()).resolves.toMatchObject({ items: [] })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/projects?page=0&size=25', expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer access-token' }) }))
  })

  it('creates only generic project fields', async () => {
    setAccessToken('access-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"id":"project-id","name":"Generic"}', { status: 201 }))
    await createProject('Generic', '')
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/projects', expect.objectContaining({ method: 'POST', body: '{"name":"Generic","description":null}' }))
  })
})
