import { describe, expect, it } from 'vitest'
import { ApiError, isUnauthorizedError } from './apiError'

describe('ApiError', () => {
  it('preserves HTTP status and distinguishes unauthorized responses', () => {
    const unauthorized = ApiError.http(401, 'Authentication failed.')
    const serverFailure = ApiError.http(500, 'Server failed.')

    expect(unauthorized.status).toBe(401)
    expect(isUnauthorizedError(unauthorized)).toBe(true)
    expect(serverFailure.status).toBe(500)
    expect(isUnauthorizedError(serverFailure)).toBe(false)
  })

  it('distinguishes network failures from HTTP failures', () => {
    const networkFailure = ApiError.network()
    expect(networkFailure.kind).toBe('network')
    expect(networkFailure.status).toBeNull()
  })
})
