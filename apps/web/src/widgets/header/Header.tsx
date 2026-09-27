import { useAuth } from '../../shared/auth/AuthProvider'

export function Header() {
  const { user } = useAuth()
  return (
    <header className="app-header">
      <div>
        <p className="header-title">Flexible Project Manager</p>
        <p className="header-organization">{user?.organization.name}</p>
      </div>
      <div className="user-context" aria-label="Current user">
        <strong>{user?.displayName}</strong>
        <span>{user?.email}</span>
      </div>
    </header>
  )
}
