import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function DashboardPage() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  return <section className="page-section"><p className="eyebrow">{t('dashboard.title')}</p><h1>{t('dashboard.welcome')}, {user?.displayName}</h1><p className="lead">{t('dashboard.ready')}</p></section>
}
