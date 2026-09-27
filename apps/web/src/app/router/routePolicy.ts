import type { AuthStatus } from '../../shared/auth/AuthProvider'

export function resolveRootPath(status: AuthStatus): string | null {
  if (status === 'loading') return null
  if (status === 'error') return null
  if (status === 'setup-required') return '/setup'
  if (status === 'authenticated') return '/app/dashboard'
  return '/login'
}

export function resolveProtectedPath(status: AuthStatus): string | null {
  if (status === 'loading') return null
  if (status === 'error') return null
  if (status === 'setup-required') return '/setup'
  if (status !== 'authenticated') return '/login'
  return null
}
