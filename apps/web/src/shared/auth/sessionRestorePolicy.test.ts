import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/apiError'
import { resolveRefreshFailure, resolveSetupStatusFailure } from './sessionRestorePolicy'

describe('session restoration failure policy', () => {
  it('treats refresh 401 as unauthenticated', () => {
    expect(resolveRefreshFailure(ApiError.http(401, 'Unauthorized'))).toBe('unauthenticated')
  })

  it('treats refresh server and network failures as error', () => {
    expect(resolveRefreshFailure(ApiError.http(500, 'Server error'))).toBe('error')
    expect(resolveRefreshFailure(ApiError.network())).toBe('error')
  })

  it('treats setup-status failures as error', () => {
    expect(resolveSetupStatusFailure()).toBe('error')
  })
})
