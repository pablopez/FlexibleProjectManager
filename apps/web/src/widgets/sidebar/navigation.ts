export type NavigationItem = {
  label: string
  key: 'dashboard' | 'projects' | 'users' | 'organization' | 'license' | 'settings'
  path: string
  adminOnly?: boolean
  permission?: string
}

export const navigationItems: NavigationItem[] = [
  { key: 'dashboard', label: 'Dashboard', path: '/app/dashboard' },
  { key: 'projects', label: 'Projects', path: '/app/projects' },
  { key: 'users', label: 'Users', path: '/app/users', adminOnly: true },
  { key: 'organization', label: 'Organization', path: '/app/organization', adminOnly: true },
  { key: 'license', label: 'License', path: '/app/license', permission: 'license:read' },
  { key: 'settings', label: 'Settings', path: '/app/settings' },
]

export function visibleNavigation(roles: string[], permissions: string[] = []): NavigationItem[] {
  const isAdmin = roles.includes('ADMIN')
  return navigationItems.filter((item) => (!item.adminOnly || isAdmin) && (!item.permission || permissions.includes(item.permission)))
}

export function isNavigationItemActive(pathname: string, itemPath: string): boolean {
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}
