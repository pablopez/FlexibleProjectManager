import { requestJson } from './apiError'
import { getAccessToken } from '../auth/authSession'

export type User = { id: string; email: string; displayName: string; status: 'ACTIVE' | 'DISABLED'; roles: string[]; createdAt: string; updatedAt: string }
export type UserList = { items: User[]; page: number; size: number; total: number; totalPages: number }
export type Role = { code: string; name: string; description: string | null; permissions: string[] }
const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

function headers() { return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` } }
export function listUsers(): Promise<UserList> { return requestJson(`${base}/users`, { headers: headers() }, 'Users request') }
export function listRoles(): Promise<{ items: Role[] }> { return requestJson(`${base}/roles`, { headers: headers() }, 'Roles request') }
