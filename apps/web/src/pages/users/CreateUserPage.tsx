import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createUser, listRoles, type Role } from '../../shared/api/usersApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { canUserAction } from './userPermissions'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function CreateUserPage() {
  const { user } = useAuth(); const navigate = useNavigate()
  const { t } = useUserPreferences()
  const [roles, setRoles] = useState<Role[]>([]); const [selected, setSelected] = useState<string[]>([])
  const [email, setEmail] = useState(''); const [displayName, setDisplayName] = useState(''); const [password, setPassword] = useState('')
  const [loadingRoles, setLoadingRoles] = useState(true); const [saving, setSaving] = useState(false); const [error, setError] = useState<string | null>(null)
  const allowed = canUserAction(user?.permissions ?? [], 'users:create')
  useEffect(() => { if (!allowed) return; void listRoles().then(value => setRoles(value.items)).catch(cause => setError(cause instanceof Error ? cause.message : t('users.loadingRoles'))).finally(() => setLoadingRoles(false)) }, [allowed, t])
  function toggleRole(code: string) { setSelected(current => current.includes(code) ? current.filter(value => value !== code) : [...current, code]) }
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSaving(true); setError(null); try { const created = await createUser({ email, displayName, password, roles: selected }); navigate(`/app/users/${created.id}`) } catch (cause) { setError(cause instanceof Error ? cause.message : t('users.createError')) } finally { setSaving(false) } }
  if (!allowed) return <section className="page-content"><h1>{t('common.forbidden')}</h1><p className="alert alert-error">{t('users.permissionCreate')}</p></section>
  return <section className="page-content"><div className="page-heading"><div><p className="eyebrow">{t('users.title')}</p><h1>{t('users.newTitle')}</h1></div></div>{error && <p className="alert alert-error">{error}</p>}<div className="card"><form className="stack-form" onSubmit={submit}><label>{t('users.email')}<input required type="email" value={email} onChange={event => setEmail(event.target.value)} /></label><label>{t('users.displayName')}<input required maxLength={200} value={displayName} onChange={event => setDisplayName(event.target.value)} /></label><label>{t('users.password')}<input required minLength={8} type="password" value={password} onChange={event => setPassword(event.target.value)} /></label><fieldset><legend>{t('users.roles')}</legend>{loadingRoles ? <p className="muted">{t('users.loadingRoles')}</p> : roles.map(role => <label key={role.code}><input type="checkbox" checked={selected.includes(role.code)} onChange={() => toggleRole(role.code)} /> {role.name}<span className="muted"> — {role.permissions.join(', ')}</span></label>)}</fieldset><div><button type="submit" disabled={saving || selected.length === 0}>{saving ? t('users.creating') : t('users.create')}</button> <Link to="/app/users">{t('common.cancel')}</Link></div></form></div></section>
}
