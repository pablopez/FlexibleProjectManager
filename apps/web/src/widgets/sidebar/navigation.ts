export type NavigationItem = {
  label: string
  path: string
  adminOnly?: boolean
  permission?: string
}

export const navigationItems: NavigationItem[] = [
  { label: 'Dashboard', path: '/app/dashboard' },
  { label: 'Projects', path: '/app/projects' },
  { label: 'Users', path: '/app/users', adminOnly: true },
  { label: 'Organization', path: '/app/organization', adminOnly: true },
  { label: 'License', path: '/app/license', permission: 'license:read' },
  { label: 'Settings', path: '/app/settings' },
]

export function visibleNavigation(roles: string[], permissions: string[] = []): NavigationItem[] {
  const isAdmin = roles.includes('ADMIN')
  return navigationItems.filter((item) => (!item.adminOnly || isAdmin) && (!item.permission || permissions.includes(item.permission)))
}

export function isNavigationItemActive(pathname: string, itemPath: string): boolean {
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}
