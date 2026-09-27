import { describe, expect, it } from 'vitest'
import { resolveProtectedPath, resolveRootPath } from './routePolicy'

describe('application route policy', () => {
  it('routes startup states without rendering protected content while loading', () => {
    expect(resolveRootPath('loading')).toBeNull()
    expect(resolveRootPath('error')).toBeNull()
    expect(resolveRootPath('setup-required')).toBe('/setup')
    expect(resolveRootPath('unauthenticated')).toBe('/login')
    expect(resolveRootPath('authenticated')).toBe('/app/dashboard')
  })

  it('protects application routes and keeps authenticated routes available', () => {
    expect(resolveProtectedPath('loading')).toBeNull()
    expect(resolveProtectedPath('error')).toBeNull()
    expect(resolveProtectedPath('unauthenticated')).toBe('/login')
    expect(resolveProtectedPath('setup-required')).toBe('/setup')
    expect(resolveProtectedPath('authenticated')).toBeNull()
  })
})
