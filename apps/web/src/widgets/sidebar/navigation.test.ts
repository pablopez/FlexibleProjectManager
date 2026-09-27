import { describe, expect, it } from 'vitest'
import { isNavigationItemActive, visibleNavigation } from './navigation'

describe('shell navigation', () => {
  it('shows all shell entries to administrators', () => {
    expect(visibleNavigation(['ADMIN']).map((item) => item.label)).toEqual([
      'Dashboard', 'Projects', 'Users', 'Organization', 'License', 'Settings',
    ])
  })

  it('hides administrative entries from regular roles', () => {
    expect(visibleNavigation(['USER']).map((item) => item.label)).toEqual(['Dashboard', 'Projects', 'Settings'])
    expect(visibleNavigation(['VIEWER']).map((item) => item.label)).toEqual(['Dashboard', 'Projects', 'Settings'])
  })

  it('marks the exact route and nested route as active', () => {
    expect(isNavigationItemActive('/app/projects', '/app/projects')).toBe(true)
    expect(isNavigationItemActive('/app/projects/detail', '/app/projects')).toBe(true)
    expect(isNavigationItemActive('/app/settings', '/app/projects')).toBe(false)
  })
})
