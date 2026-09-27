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

async function readResponse<T>(response: Response): Promise<T> {
  const body = (await response.json()) as T & { message?: string }
  if (!response.ok) {
    throw new Error(body.message ?? `Authentication request failed with HTTP ${response.status}.`)
  }
  return body
}

export async function login(email: string, password: string): Promise<TokenResponse> {
  return readResponse(await fetch(`${apiBaseUrl}/auth/login`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  }))
}

export async function refreshSession(): Promise<TokenResponse> {
  return readResponse(await fetch(`${apiBaseUrl}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  }))
}

export async function logout(): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new Error(`Logout failed with HTTP ${response.status}.`)
  }
}

export async function getCurrentUser(token: string): Promise<CurrentUserResponse> {
  return readResponse(await fetch(`${apiBaseUrl}/auth/me`, {
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
  }))
}
