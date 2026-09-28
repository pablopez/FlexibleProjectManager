import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listProjects, type Project } from '../../shared/api/projectsApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canProjectAction } from './projectPermissions'

type Filter = 'ALL' | Project['status']

export function ProjectsPage() {
  const { user } = useAuth()
  const [filter, setFilter] = useState<Filter>('ALL')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<{ items: Project[]; totalPages: number; total: number } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const canCreate = canProjectAction(user?.permissions ?? [], 'projects:create')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    void listProjects(page, 25, filter === 'ALL' ? undefined : filter)
      .then(value => { if (active) setResult(value) })
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : 'Unable to load projects.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filter, page])

  function changeFilter(value: Filter) { setFilter(value); setPage(0) }

  return <section className="page-content">
    <div className="page-heading">
      <div><p className="eyebrow">Platform Core</p><h1>Projects</h1><p className="muted">Generic projects identified by a stable platform ID.</p></div>
      {canCreate && <Link className="button" to="/app/projects/new">New project</Link>}
    </div>
    <div className="filter-row" aria-label="Project status filter">
      {(['ALL', 'ACTIVE', 'ARCHIVED'] as Filter[]).map(value => <button key={value} className={filter === value ? 'selected' : ''} type="button" onClick={() => changeFilter(value)}>{value[0] + value.slice(1).toLowerCase()}</button>)}
    </div>
    {loading && <p className="muted" aria-live="polite">Loading projects…</p>}
    {error && <p className="alert alert-error">{error}</p>}
    {!loading && !error && result && result.items.length === 0 && <p className="muted empty-state">No projects found.</p>}
    {!loading && !error && result && result.items.length > 0 && <>
      <div className="card table-card"><table><thead><tr><th>Name</th><th>Description</th><th>Status</th></tr></thead><tbody>{result.items.map(project => <tr key={project.id}><td><Link to={`/app/projects/${project.id}`}>{project.name}</Link></td><td>{project.description ?? '—'}</td><td>{project.status}</td></tr>)}</tbody></table></div>
      <div className="pagination"><button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, result.totalPages)} ({result.total})</span><button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>Next</button></div>
    </>}
  </section>
}
