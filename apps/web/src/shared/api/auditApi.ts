import { requestJson } from './apiError'

export type AuditEntry = {
  id: string
  actor: { id: string; displayName: string } | null
  action: string
  resourceType: string
  resourceId: string | null
  metadata: { changedFields: string[] } | null
  createdAt: string
}

export type AuditPage = { items: AuditEntry[]; page: number; size: number; total: number; totalPages: number }
export type AuditFilters = { userId?: string; action?: string; resourceType?: string; resourceId?: string; from?: string; to?: string }

const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
function headers() { const token = sessionStorage.getItem('fpm.accessToken'); return { Accept: 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) } }

export function listAudit(page = 0, size = 25, filters: AuditFilters = {}): Promise<AuditPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => { if (value) query.set(key, value) })
  return requestJson(`${base}/audit?${query.toString()}`, { headers: headers() }, 'Audit request')
}
