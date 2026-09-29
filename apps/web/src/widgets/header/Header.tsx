import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function Header() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  return (
    <header className="app-header">
      <div>
        <p className="header-title">Flexible Project Manager</p>
        <p className="header-organization">{user?.organization.name}</p>
      </div>
       <div className="user-context" aria-label={t('header.currentUser')}>
        <strong>{user?.displayName}</strong>
        <span>{user?.email}</span>
      </div>
    </header>
  )
}
