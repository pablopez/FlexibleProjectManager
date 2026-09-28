import { getAccessToken } from '../auth/authSession'
import { requestJson } from './apiError'

export type Installation = { id: string; organizationId: string; name: string; platform: 'WINDOWS' | 'LINUX' | 'CLOUD' | 'OTHER'; applicationVersion: string; status: 'ACTIVE' | 'DISABLED' | 'UNLICENSED'; createdAt: string; lastSeenAt: string | null }
const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
function headers() { return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` } }
export function getInstallation(): Promise<Installation> { return requestJson(`${base}/installation`, { headers: headers() }, 'Installation request') }
export function updateInstallation(input: { name: string }): Promise<Installation> {
  return requestJson(`${base}/installation`, { method: 'PATCH', headers: { ...headers(), 'Content-Type': 'application/json' }, body: JSON.stringify(input) }, 'Update installation request')
}
