import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { archiveProject, getProject, restoreProject, updateProject, type Project } from '../../shared/api/projectsApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canProjectAction } from './projectPermissions'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const { user } = useAuth()
  const { t } = useUserPreferences()
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
     void getProject(projectId).then(value => { setProject(value); setName(value.name); setDescription(value.description ?? '') }).catch(cause => setError(cause instanceof Error ? cause.message : t('projects.loadError'))).finally(() => setLoading(false))
  }, [projectId])

  async function save() {
    if (!projectId) return
    setSaving(true); setError(null)
    try { setProject(await updateProject(projectId, name, description || null)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : t('projects.updateError')) }
    finally { setSaving(false) }
  }

  async function changeStatus() {
    if (!project || !projectId) return
    setSaving(true); setError(null)
    try { setProject(project.status === 'ACTIVE' ? await archiveProject(projectId) : await restoreProject(projectId)) }
    catch (cause) { setError(cause instanceof Error ? cause.message : t('projects.statusError')) }
    finally { setSaving(false) }
  }

  if (loading) return <section className="page-content"><p className="muted">{t('projects.loadingOne')}</p></section>
  if (!project) return <section className="page-content"><p className="alert alert-error">{error ?? t('projects.notFound')}</p><Link to="/app/projects">{t('projects.back')}</Link></section>
  return <section className="page-content"><div className="page-heading"><div><p className="eyebrow">{t('projects.project')}</p><h1>{project.name}</h1><p className="muted">{project.id}</p></div><Link to="/app/projects">{t('projects.back')}</Link></div>{error && <p className="alert alert-error">{error}</p>}<div className="card"><p>{t('projects.status')}: <strong>{project.status}</strong></p>{canUpdate ? <div className="stack-form"><label>{t('projects.name')}<input value={name} maxLength={200} onChange={event => setName(event.target.value)} /></label><label>{t('projects.descriptionField')}<textarea value={description} maxLength={2000} onChange={event => setDescription(event.target.value)} /></label><button type="button" disabled={saving} onClick={() => void save()}>{t('projects.save')}</button></div> : <><p>{t('projects.name')}: {project.name}</p><p>{t('projects.descriptionField')}: {project.description ?? '—'}</p></>}{canArchive && <button type="button" disabled={saving} onClick={() => void changeStatus()}>{project.status === 'ACTIVE' ? t('projects.archive') : t('projects.restore')}</button>}</div></section>
}
