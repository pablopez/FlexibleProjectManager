import { isUnauthorizedError } from '../api/apiError'

export type RestoreFailureStatus = 'unauthenticated' | 'error'

export function resolveRefreshFailure(error: unknown): RestoreFailureStatus {
  return isUnauthorizedError(error) ? 'unauthenticated' : 'error'
}

export function resolveSetupStatusFailure(): 'error' {
  return 'error'
}
