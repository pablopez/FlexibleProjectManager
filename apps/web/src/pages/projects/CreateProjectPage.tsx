import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createProject } from '../../shared/api/projectsApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canProjectAction } from './projectPermissions'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function CreateProjectPage() {
  const { user } = useAuth()
  const { t } = useUserPreferences()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  if (!canProjectAction(user?.permissions ?? [], 'projects:create')) return <section className="page-content"><h1>{t('common.forbidden')}</h1><p className="alert alert-error">{t('projects.permissionCreate')}</p></section>

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError(null)
    try { const project = await createProject(name, description); navigate(`/app/projects/${project.id}`) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to create project.') }
    finally { setSaving(false) }
  }

  return <section className="page-content"><div className="page-heading"><div><p className="eyebrow">{t('projects.title')}</p><h1>{t('projects.newTitle')}</h1></div></div>{error && <p className="alert alert-error">{error}</p>}<div className="card"><form className="stack-form" onSubmit={submit}><label>{t('projects.projectName')}<input required maxLength={200} value={name} onChange={event => setName(event.target.value)} /></label><label>{t('projects.descriptionField')}<textarea maxLength={2000} value={description} onChange={event => setDescription(event.target.value)} /></label><div><button type="submit" disabled={saving}>{saving ? t('projects.creating') : t('projects.create')}</button> <Link to="/app/projects">{t('common.cancel')}</Link></div></form></div></section>
}
