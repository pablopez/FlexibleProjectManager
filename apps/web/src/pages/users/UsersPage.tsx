import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listUsers, type User, type UserStatus } from '../../shared/api/usersApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canUserAction } from './userPermissions'

type Filter = 'ALL' | UserStatus

export function UsersPage() {
  const { user } = useAuth()
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
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : 'Unable to load users.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [canRead, filter, page])

  if (!canRead) return <section className="page-content"><h1>Forbidden</h1><p className="alert alert-error">You do not have permission to view users.</p></section>
  function changeFilter(value: Filter) { setFilter(value); setPage(0) }

  return <section className="page-content">
    <div className="page-heading"><div><p className="eyebrow">Access control</p><h1>Users</h1><p className="muted">Manage members of this organization.</p></div>{canCreate && <Link className="button" to="/app/users/new">New user</Link>}</div>
    <div className="filter-row" aria-label="User status filter">{(['ALL', 'ACTIVE', 'DISABLED'] as Filter[]).map(value => <button key={value} className={filter === value ? 'selected' : ''} type="button" onClick={() => changeFilter(value)}>{value === 'ALL' ? 'All' : value === 'ACTIVE' ? 'Active' : 'Disabled'}</button>)}</div>
    {loading && <p className="muted" aria-live="polite">Loading users…</p>}
    {error && <p className="alert alert-error">{error}</p>}
    {!loading && !error && result && result.items.length === 0 && <p className="muted empty-state">No users found.</p>}
    {!loading && !error && result && result.items.length > 0 && <><div className="card table-card"><table><thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Roles</th></tr></thead><tbody>{result.items.map(value => <tr key={value.id}><td><Link to={`/app/users/${value.id}`}>{value.displayName}</Link></td><td>{value.email}</td><td><span className={`status status-${value.status.toLowerCase()}`}>{value.status}</span></td><td>{value.roles.join(', ')}</td></tr>)}</tbody></table></div><div className="pagination"><button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, result.totalPages)} ({result.total})</span><button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>Next</button></div></>}
  </section>
}
