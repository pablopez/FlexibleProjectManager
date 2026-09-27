import { afterEach, describe, expect, it } from 'vitest'
import { clearAccessToken, getAccessToken, setAccessToken } from './authSession'

describe('authSession', () => {
  afterEach(() => clearAccessToken())

  it('keeps the access token in memory only', () => {
    setAccessToken('memory-token')
    expect(getAccessToken()).toBe('memory-token')
    clearAccessToken()
    expect(getAccessToken()).toBeNull()
  })
})
