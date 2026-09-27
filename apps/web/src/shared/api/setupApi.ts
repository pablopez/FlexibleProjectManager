import { requestJson } from './apiError'

export type SetupStatusResponse = { initialized: boolean }

export type InitializeSetupRequest = {
  organization: { name: string }
  installation: { name: string }
  administrator: { email: string; displayName: string; password: string }
}

export type SetupInitializationResponse = {
  initialized: true
  organization: { id: string; name: string }
  installation: {
    id: string
    organizationId: string
    name: string
    platform: string
    applicationVersion: string
    status: string
    createdAt: string
    lastSeenAt: string | null
  }
  administrator: { id: string; email: string; displayName: string }
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export async function getSetupStatus(): Promise<SetupStatusResponse> {
  return requestJson(`${apiBaseUrl}/setup/status`, { headers: { Accept: 'application/json' } }, 'Setup request')
}

export async function initializeSetup(request: InitializeSetupRequest): Promise<SetupInitializationResponse> {
  return requestJson(`${apiBaseUrl}/setup/initialize`, {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  }, 'Setup request')
}
