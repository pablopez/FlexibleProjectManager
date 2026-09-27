import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getSetupStatus, initializeSetup, type InitializeSetupRequest } from '../api/setupApi'
import { getCurrentUser, login as loginApi, logout as logoutApi, refreshSession, type CurrentUserResponse } from '../api/authApi'
import { resolveRefreshFailure, resolveSetupStatusFailure } from './sessionRestorePolicy'
import { clearAccessToken, getAccessToken, setAccessToken } from './authSession'

export type AuthStatus = 'loading' | 'setup-required' | 'unauthenticated' | 'authenticated' | 'error'

type AuthContextValue = {
  status: AuthStatus
  user: CurrentUserResponse | null
  error: string | null
  login: (email: string, password: string) => Promise<void>
  initialize: (request: InitializeSetupRequest) => Promise<void>
  logout: () => Promise<void>
  retry: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<CurrentUserResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const restoreSession = useCallback(async () => {
    setStatus('loading')
    setError(null)
    let setupInitialized: boolean
    try {
      setupInitialized = (await getSetupStatus()).initialized
    } catch {
      clearAccessToken()
      setUser(null)
      setError('Unable to connect to Flexible Project Manager.')
      setStatus(resolveSetupStatusFailure())
      return
    }

    if (!setupInitialized) {
      clearAccessToken()
      setUser(null)
      setStatus('setup-required')
      return
    }

    let accessToken: string
    try {
      accessToken = (await refreshSession()).accessToken
    } catch (cause) {
      clearAccessToken()
      setUser(null)
      if (resolveRefreshFailure(cause) === 'error') {
        setError('Unable to connect to Flexible Project Manager.')
      }
      setStatus(resolveRefreshFailure(cause))
      return
    }

    try {
      setAccessToken(accessToken)
      setUser(await getCurrentUser(accessToken))
      setStatus('authenticated')
    } catch (cause) {
      clearAccessToken()
      setUser(null)
      if (resolveRefreshFailure(cause) === 'error') {
        setError('Unable to connect to Flexible Project Manager.')
      }
      setStatus(resolveRefreshFailure(cause))
    }
  }, [])

  useEffect(() => {
    void restoreSession()
  }, [restoreSession])

  const login = useCallback(async (email: string, password: string) => {
    setError(null)
    try {
      const tokenResponse = await loginApi(email, password)
      setAccessToken(tokenResponse.accessToken)
      setUser(await getCurrentUser(tokenResponse.accessToken))
      setStatus('authenticated')
    } catch (cause) {
      clearAccessToken()
      setUser(null)
      setError(errorMessage(cause, 'Authentication failed.'))
      setStatus('unauthenticated')
      throw cause
    }
  }, [])

  const initialize = useCallback(async (request: InitializeSetupRequest) => {
    setError(null)
    try {
      await initializeSetup(request)
      clearAccessToken()
      setUser(null)
      setStatus('unauthenticated')
    } catch (cause) {
      setError(errorMessage(cause, 'Setup failed.'))
      throw cause
    }
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } finally {
      clearAccessToken()
      setUser(null)
      setStatus('unauthenticated')
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({ status, user, error, login, initialize, logout, retry: restoreSession }), [status, user, error, login, initialize, logout, restoreSession])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider.')
  }
  return context
}

export function hasAccessToken(): boolean {
  return getAccessToken() !== null
}
