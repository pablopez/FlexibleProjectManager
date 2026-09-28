import { getAccessToken } from '../auth/authSession'
import { requestJson } from './apiError'

export type Organization = { id: string; name: string; slug: string | null; status: 'ACTIVE' | 'DISABLED'; createdAt: string; updatedAt: string }
const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
function headers() { return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` } }
function jsonHeaders() { return { ...headers(), 'Content-Type': 'application/json' } }
export function getOrganization(): Promise<Organization> { return requestJson(`${base}/organization`, { headers: headers() }, 'Organization request') }
export function updateOrganization(input: { name?: string; slug?: string | null }): Promise<Organization> {
  return requestJson(`${base}/organization`, { method: 'PATCH', headers: jsonHeaders(), body: JSON.stringify(input) }, 'Update organization request')
}
