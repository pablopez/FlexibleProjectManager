import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listUsers, type User, type UserStatus } from '../../shared/api/usersApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canUserAction } from './userPermissions'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

type Filter = 'ALL' | UserStatus

export function UsersPage() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  const [filter, setFilter] = useState<Filter>('ALL')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<{ items: User[]; totalPages: number; total: number } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const canRead = canUserAction(user?.permissions ?? [], 'users:read')
  const canCreate = canUserAction(user?.permissions ?? [], 'users:create')

  useEffect(() => {
    if (!canRead) return
    let active = true
    setLoading(true); setError(null)
    void listUsers(page, 25, filter === 'ALL' ? undefined : filter)
      .then(value => { if (active) setResult(value) })
       .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : t('users.loadError')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [canRead, filter, page])

  if (!canRead) return <section className="page-content"><h1>{t('common.forbidden')}</h1><p className="alert alert-error">{t('users.permissionView')}</p></section>
  function changeFilter(value: Filter) { setFilter(value); setPage(0) }

  return <section className="page-content">
     <div className="page-heading"><div><p className="eyebrow">{t('users.eyebrow')}</p><h1>{t('users.title')}</h1><p className="muted">{t('users.description')}</p></div>{canCreate && <Link className="button" to="/app/users/new">{t('users.new')}</Link>}</div>
     <div className="filter-row" aria-label={t('users.status')}>{(['ALL', 'ACTIVE', 'DISABLED'] as Filter[]).map(value => <button key={value} className={filter === value ? 'selected' : ''} type="button" onClick={() => changeFilter(value)}>{value === 'ALL' ? t('common.all') : value === 'ACTIVE' ? t('common.active') : t('common.disabled')}</button>)}</div>
     {loading && <p className="muted" aria-live="polite">{t('users.loading')}</p>}
    {error && <p className="alert alert-error">{error}</p>}
     {!loading && !error && result && result.items.length === 0 && <p className="muted empty-state">{t('users.empty')}</p>}
     {!loading && !error && result && result.items.length > 0 && <><div className="card table-card"><table><thead><tr><th>{t('users.name')}</th><th>{t('users.email')}</th><th>{t('users.status')}</th><th>{t('users.roles')}</th></tr></thead><tbody>{result.items.map(value => <tr key={value.id}><td><Link to={`/app/users/${value.id}`}>{value.displayName}</Link></td><td>{value.email}</td><td><span className={`status status-${value.status.toLowerCase()}`}>{value.status}</span></td><td>{value.roles.join(', ')}</td></tr>)}</tbody></table></div><div className="pagination"><button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>{t('common.previous')}</button><span>{t('users.page')} {page + 1} {t('users.of')} {Math.max(1, result.totalPages)} ({result.total})</span><button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>{t('common.next')}</button></div></>}
  </section>
}
