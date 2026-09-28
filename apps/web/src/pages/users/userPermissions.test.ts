import { describe, expect, it } from 'vitest'
import { canUserAction } from './userPermissions'

describe('user permissions', () => {
  it('uses effective permission codes rather than role names', () => {
    expect(canUserAction(['users:read'], 'users:read')).toBe(true)
    expect(canUserAction(['ADMIN'], 'users:read')).toBe(false)
  })
})
