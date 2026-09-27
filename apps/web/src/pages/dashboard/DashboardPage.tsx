import { useAuth } from '../../shared/auth/AuthProvider'

export function DashboardPage() {
  const { user } = useAuth()
  return <section className="page-section"><p className="eyebrow">Dashboard</p><h1>Welcome, {user?.displayName}</h1><p className="lead">Flexible Project Manager is ready.</p></section>
}
