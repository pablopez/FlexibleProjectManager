import { describe, expect, it } from 'vitest'
import { isNavigationItemActive, visibleNavigation } from './navigation'

describe('shell navigation', () => {
  it('shows all shell entries to administrators', () => {
    expect(visibleNavigation(['ADMIN'], ['license:read']).map((item) => item.label)).toEqual([
      'Dashboard', 'Projects', 'Users', 'Organization', 'License', 'Settings',
    ])
  })

  it('hides administrative entries from regular roles', () => {
    expect(visibleNavigation(['USER']).map((item) => item.label)).toEqual(['Dashboard', 'Projects', 'Settings'])
    expect(visibleNavigation(['VIEWER']).map((item) => item.label)).toEqual(['Dashboard', 'Projects', 'Settings'])
  })

  it('shows licensing from permission rather than role name', () => {
    expect(visibleNavigation(['USER'], ['license:read']).map((item) => item.label)).toContain('License')
  })

  it('shows audit only with audit:read', () => {
    expect(visibleNavigation(['USER'], ['audit:read']).map((item) => item.label)).toContain('Audit')
    expect(visibleNavigation(['ADMIN'], ['license:read']).map((item) => item.label)).not.toContain('Audit')
  })

  it('marks the exact route and nested route as active', () => {
    expect(isNavigationItemActive('/app/projects', '/app/projects')).toBe(true)
    expect(isNavigationItemActive('/app/projects/detail', '/app/projects')).toBe(true)
    expect(isNavigationItemActive('/app/settings', '/app/projects')).toBe(false)
  })
})
