import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createProject } from '../../shared/api/projectsApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canProjectAction } from './projectPermissions'

export function CreateProjectPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  if (!canProjectAction(user?.permissions ?? [], 'projects:create')) return <section className="page-content"><h1>Forbidden</h1><p className="alert alert-error">You do not have permission to create projects.</p></section>

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(null)
    try { const project = await createProject(name, description); navigate(`/app/projects/${project.id}`) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to create project.') }
    finally { setSaving(false) }
  }

  return <section className="page-content"><div className="page-heading"><div><p className="eyebrow">Projects</p><h1>New project</h1></div></div>{error && <p className="alert alert-error">{error}</p>}<div className="card"><form className="stack-form" onSubmit={submit}><label>Project name<input required maxLength={200} value={name} onChange={event => setName(event.target.value)} /></label><label>Description<textarea maxLength={2000} value={description} onChange={event => setDescription(event.target.value)} /></label><div><button type="submit" disabled={saving}>{saving ? 'Creating…' : 'Create project'}</button> <Link to="/app/projects">Cancel</Link></div></form></div></section>
}
