export function canProjectAction(permissions: string[], permission: string): boolean {
  return permissions.includes(permission)
}
