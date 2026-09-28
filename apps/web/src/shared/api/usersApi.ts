import { requestJson } from './apiError'
import { getAccessToken } from '../auth/authSession'

export type UserStatus = 'ACTIVE' | 'DISABLED'
export type User = { id: string; email: string; displayName: string; status: UserStatus; roles: string[]; createdAt: string; updatedAt: string }
export type UserList = { items: User[]; page: number; size: number; total: number; totalPages: number }
export type Role = { code: string; name: string; description: string | null; permissions: string[] }
const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

function headers(json = false) {
  return { Accept: 'application/json', ...(json ? { 'Content-Type': 'application/json' } : {}), Authorization: `Bearer ${getAccessToken() ?? ''}` }
}

export function listUsers(page = 0, size = 25, status?: UserStatus): Promise<UserList> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) query.set('status', status)
  return requestJson(`${base}/users?${query.toString()}`, { headers: headers() }, 'Users request')
}
export function getUser(userId: string): Promise<User> { return requestJson(`${base}/users/${userId}`, { headers: headers() }, 'User request') }
export function listRoles(): Promise<{ items: Role[] }> { return requestJson(`${base}/roles`, { headers: headers() }, 'Roles request') }
export function createUser(input: { email: string; displayName: string; password: string; roles: string[] }): Promise<User> {
  return requestJson(`${base}/users`, { method: 'POST', headers: headers(true), body: JSON.stringify(input) }, 'Create user request')
}
export function updateUser(userId: string, input: { displayName?: string; status?: UserStatus }): Promise<User> {
  return requestJson(`${base}/users/${userId}`, { method: 'PATCH', headers: headers(true), body: JSON.stringify(input) }, 'Update user request')
}
export function replaceUserRoles(userId: string, roles: string[]): Promise<User> {
  return requestJson(`${base}/users/${userId}/roles`, { method: 'PUT', headers: headers(true), body: JSON.stringify({ roles }) }, 'Update user roles request')
}
