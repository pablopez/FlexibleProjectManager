import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listAudit } from './auditApi'

describe('audit api', () => {
  beforeEach(() => { vi.restoreAllMocks(); sessionStorage.setItem('fpm.accessToken', 'audit-token') })
  it('constructs the documented filters without organization selection', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"items":[],"page":0,"size":25,"total":0,"totalPages":0}', { status: 200 })))
    await listAudit(0, 25, { action: 'PROJECT_UPDATED', resourceType: 'PROJECT', resourceId: 'r', userId: 'u', from: '2026-01-01T00:00:00Z', to: '2026-01-02T00:00:00Z' })
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/v1/audit?page=0&size=25&action=PROJECT_UPDATED'), expect.objectContaining({ headers: { Accept: 'application/json', Authorization: 'Bearer audit-token' } }))
    expect(String(vi.mocked(fetch).mock.calls[0][0])).not.toContain('organizationId')
  })
})
