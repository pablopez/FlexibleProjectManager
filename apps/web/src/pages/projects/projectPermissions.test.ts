import { describe, expect, it } from 'vitest'
import { canProjectAction } from './projectPermissions'

describe('project permissions', () => {
  it('allows ADMIN mutations', () => {
    const permissions = ['projects:create', 'projects:update', 'projects:archive']
    expect(canProjectAction(permissions, 'projects:create')).toBe(true)
    expect(canProjectAction(permissions, 'projects:update')).toBe(true)
    expect(canProjectAction(permissions, 'projects:archive')).toBe(true)
  })

  it('allows USER create and edit but not archive', () => {
    const permissions = ['projects:create', 'projects:update']
    expect(canProjectAction(permissions, 'projects:create')).toBe(true)
    expect(canProjectAction(permissions, 'projects:update')).toBe(true)
    expect(canProjectAction(permissions, 'projects:archive')).toBe(false)
  })

  it('allows VIEWER no mutation actions', () => {
    const permissions = ['projects:read']
    expect(canProjectAction(permissions, 'projects:create')).toBe(false)
    expect(canProjectAction(permissions, 'projects:update')).toBe(false)
    expect(canProjectAction(permissions, 'projects:archive')).toBe(false)
  })
})
