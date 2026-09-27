export type NavigationItem = {
  label: string
  path: string
  adminOnly?: boolean
}

export const navigationItems: NavigationItem[] = [
  { label: 'Dashboard', path: '/app/dashboard' },
  { label: 'Projects', path: '/app/projects' },
  { label: 'Users', path: '/app/users', adminOnly: true },
  { label: 'Organization', path: '/app/organization', adminOnly: true },
  { label: 'License', path: '/app/license', adminOnly: true },
  { label: 'Settings', path: '/app/settings' },
]

export function visibleNavigation(roles: string[]): NavigationItem[] {
  const isAdmin = roles.includes('ADMIN')
  return navigationItems.filter((item) => !item.adminOnly || isAdmin)
}

export function isNavigationItemActive(pathname: string, itemPath: string): boolean {
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}
