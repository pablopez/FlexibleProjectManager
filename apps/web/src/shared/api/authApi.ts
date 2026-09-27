import { request, requestJson } from './apiError'

export type TokenResponse = { accessToken: string; expiresIn: number }

export type CurrentUserResponse = {
  id: string
  email: string
  displayName: string
  organization: { id: string; name: string }
  roles: string[]
  permissions: string[]
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export async function login(email: string, password: string): Promise<TokenResponse> {
  return requestJson(`${apiBaseUrl}/auth/login`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  }, 'Authentication request')
}

export async function refreshSession(): Promise<TokenResponse> {
  return requestJson(`${apiBaseUrl}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  }, 'Authentication request')
}

export async function logout(): Promise<void> {
  await request(`${apiBaseUrl}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  }, 'Logout request')
}

export async function getCurrentUser(token: string): Promise<CurrentUserResponse> {
  return requestJson(`${apiBaseUrl}/auth/me`, {
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
  }, 'Current-user request')
}
