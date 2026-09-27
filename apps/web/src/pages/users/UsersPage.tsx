import { useEffect, useState } from 'react'
import { listUsers, type User } from '../../shared/api/usersApi'

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { void listUsers().then(result => setUsers(result.items)).catch(cause => setError(cause instanceof Error ? cause.message : 'Unable to load users.')) }, [])
  return <section className="page-content">
    <div className="page-heading"><div><p className="eyebrow">Access control</p><h1>Users</h1><p className="muted">Manage members of this organization.</p></div></div>
    {error && <p className="alert alert-error">{error}</p>}
    {!error && <div className="card table-card"><table><thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Roles</th></tr></thead><tbody>{users.map(user => <tr key={user.id}><td>{user.displayName}</td><td>{user.email}</td><td><span className={`status status-${user.status.toLowerCase()}`}>{user.status}</span></td><td>{user.roles.join(', ')}</td></tr>)}</tbody></table>{users.length === 0 && <p className="muted empty-state">No users found.</p>}</div>}
  </section>
}
