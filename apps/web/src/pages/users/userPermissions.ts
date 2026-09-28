export function canUserAction(permissions: string[], permission: string): boolean {
  return permissions.includes(permission)
}
