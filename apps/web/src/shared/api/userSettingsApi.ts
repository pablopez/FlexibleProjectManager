import { getAccessToken } from '../auth/authSession'
import { requestJson } from './apiError'

export type UserSettings = { language: 'en' | 'es'; theme: 'light' | 'dark' | 'system' }
export type UserSettingsPatch = Partial<UserSettings>

const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

function headers() {
  return { Accept: 'application/json', Authorization: `Bearer ${getAccessToken() ?? ''}` }
}

export function getUserSettings(): Promise<UserSettings> {
  return requestJson(`${base}/settings/user`, { headers: headers() }, 'User settings request')
}

export function updateUserSettings(input: UserSettingsPatch): Promise<UserSettings> {
  return requestJson(`${base}/settings/user`, {
    method: 'PATCH',
    headers: { ...headers(), 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }, 'Update user settings request')
}
