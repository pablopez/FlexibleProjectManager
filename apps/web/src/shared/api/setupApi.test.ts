import { afterEach, describe, expect, it, vi } from 'vitest'
import { getSetupStatus, initializeSetup } from './setupApi'

describe('setupApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('reads an uninitialized status', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"initialized":false}', { status: 200 }))
    await expect(getSetupStatus()).resolves.toEqual({ initialized: false })
  })

  it('submits the documented setup request and returns the response', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('{"initialized":true}', { status: 201 }),
    )
    const request = {
      organization: { name: 'Example Studio' },
      installation: { name: 'Main Workstation' },
      administrator: { email: 'admin@example.com', displayName: 'Administrator', password: 'password123' },
    }
    await expect(initializeSetup(request)).resolves.toEqual({ initialized: true })
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/setup/initialize', expect.objectContaining({
      method: 'POST', body: JSON.stringify(request),
    }))
  })

  it('surfaces setup API errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(
      '{"code":"SYSTEM_ALREADY_INITIALIZED","message":"Already initialized."}', { status: 409 },
    ))
    await expect(getSetupStatus()).rejects.toMatchObject({ kind: 'http', status: 409, message: 'Already initialized.' })
  })

  it('preserves setup-status server and network failures', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response('', { status: 500 }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(getSetupStatus()).rejects.toMatchObject({ kind: 'http', status: 500 })
    await expect(getSetupStatus()).rejects.toMatchObject({ kind: 'network', status: null })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
