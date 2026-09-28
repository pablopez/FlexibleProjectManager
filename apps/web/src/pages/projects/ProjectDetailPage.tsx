import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { archiveProject, getProject, restoreProject, updateProject, type Project } from '../../shared/api/projectsApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canProjectAction } from './projectPermissions'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const { user } = useAuth()
  const [project, setProject] = useState<Project | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const canUpdate = canProjectAction(user?.permissions ?? [], 'projects:update')
  const canArchive = canProjectAction(user?.permissions ?? [], 'projects:archive')

  useEffect(() => {
    if (!projectId) return
    void getProject(projectId).then(value => { setProject(value); setName(value.name); setDescription(value.description ?? '') }).catch(cause => setError(cause instanceof Error ? cause.message : 'Unable to load project.')).finally(() => setLoading(false))
  }, [projectId])

  async function save() {
    if (!projectId) return
    setSaving(true); setError(null)
    try { setProject(await updateProject(projectId, name, description || null)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to update project.') }
    finally { setSaving(false) }
  }

  async function changeStatus() {
    if (!project || !projectId) return
    setSaving(true); setError(null)
    try { setProject(project.status === 'ACTIVE' ? await archiveProject(projectId) : await restoreProject(projectId)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to change project status.') }
    finally { setSaving(false) }
  }

  if (loading) return <section className="page-content"><p className="muted">Loading project…</p></section>
  if (!project) return <section className="page-content"><p className="alert alert-error">{error ?? 'Project not found.'}</p><Link to="/app/projects">Back to projects</Link></section>
  return <section className="page-content"><div className="page-heading"><div><p className="eyebrow">Project</p><h1>{project.name}</h1><p className="muted">{project.id}</p></div><Link to="/app/projects">Back to projects</Link></div>{error && <p className="alert alert-error">{error}</p>}<div className="card"><p>Status: <strong>{project.status}</strong></p>{canUpdate ? <div className="stack-form"><label>Name<input value={name} maxLength={200} onChange={event => setName(event.target.value)} /></label><label>Description<textarea value={description} maxLength={2000} onChange={event => setDescription(event.target.value)} /></label><button type="button" disabled={saving} onClick={() => void save()}>Save changes</button></div> : <><p>Name: {project.name}</p><p>Description: {project.description ?? '—'}</p></>}{canArchive && <button type="button" disabled={saving} onClick={() => void changeStatus()}>{project.status === 'ACTIVE' ? 'Archive project' : 'Restore project'}</button>}</div></section>
}
