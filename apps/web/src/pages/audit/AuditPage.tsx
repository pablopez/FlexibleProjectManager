import { useEffect, useState, type FormEvent } from 'react'
import { listAudit, type AuditFilters, type AuditPage as AuditResult } from '../../shared/api/auditApi'
import { useAuth } from '../../shared/auth/AuthProvider'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

const actions = ['PROJECT_CREATED', 'PROJECT_UPDATED', 'PROJECT_ARCHIVED', 'PROJECT_RESTORED', 'USER_CREATED', 'USER_UPDATED', 'USER_DISABLED', 'USER_REACTIVATED', 'USER_ROLES_CHANGED', 'ORGANIZATION_UPDATED', 'INSTALLATION_UPDATED', 'LICENSE_ACTIVATED', 'LICENSE_REPLACED', 'LICENSE_DEACTIVATED', 'USER_PREFERENCES_UPDATED']
const resourceTypes = ['PROJECT', 'USER', 'ORGANIZATION', 'INSTALLATION', 'LICENSE', 'USER_PREFERENCES']

export function AuditPage() {
  const { user } = useAuth(); const { t } = useUserPreferences()
  const [draft, setDraft] = useState<AuditFilters>({}); const [filters, setFilters] = useState<AuditFilters>({})
  const [page, setPage] = useState(0); const [result, setResult] = useState<AuditResult | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState<string | null>(null)
  const allowed = user?.permissions.includes('audit:read') ?? false

  useEffect(() => {
    if (!allowed) return
    let active = true; setLoading(true); setError(null)
    void listAudit(page, 25, filters).then(value => { if (active) setResult(value) }).catch(cause => { if (active) setError(cause instanceof Error ? cause.message : t('audit.loadError')) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [allowed, filters, page])

  function apply(event: FormEvent) { event.preventDefault(); setPage(0); setFilters(Object.fromEntries(Object.entries(draft).filter(([, value]) => value)) as AuditFilters) }
  function clear() { setDraft({}); setFilters({}); setPage(0) }
  function update(key: keyof AuditFilters, value: string) { setDraft(current => ({ ...current, [key]: value || undefined })) }

  if (!allowed) return <section className="page-content"><h1>{t('common.forbidden')}</h1></section>
  return <section className="page-content">
    <div className="page-heading"><div><p className="eyebrow">{t('audit.eyebrow')}</p><h1>{t('audit.title')}</h1><p className="muted">{t('audit.description')}</p></div></div>
    <form className="filter-row" onSubmit={apply} aria-label={t('audit.filters')}>
      <select aria-label={t('audit.action')} value={draft.action ?? ''} onChange={e => update('action', e.target.value)}><option value="">{t('common.all')}</option>{actions.map(action => <option key={action} value={action}>{t(`audit.action.${action}` as never)}</option>)}</select>
      <select aria-label={t('audit.resourceType')} value={draft.resourceType ?? ''} onChange={e => update('resourceType', e.target.value)}><option value="">{t('common.all')}</option>{resourceTypes.map(type => <option key={type} value={type}>{t(`audit.resource.${type}` as never)}</option>)}</select>
      <input aria-label={t('audit.userId')} placeholder={t('audit.userId')} value={draft.userId ?? ''} onChange={e => update('userId', e.target.value)} />
      <input aria-label={t('audit.resourceId')} placeholder={t('audit.resourceId')} value={draft.resourceId ?? ''} onChange={e => update('resourceId', e.target.value)} />
      <input aria-label={t('audit.from')} type="datetime-local" value={draft.from?.slice(0, 16) ?? ''} onChange={e => update('from', e.target.value ? new Date(e.target.value).toISOString() : '')} />
      <input aria-label={t('audit.to')} type="datetime-local" value={draft.to?.slice(0, 16) ?? ''} onChange={e => update('to', e.target.value ? new Date(e.target.value).toISOString() : '')} />
      <button type="submit">{t('audit.apply')}</button><button type="button" onClick={clear}>{t('audit.clear')}</button>
    </form>
    {loading && <p className="muted">{t('audit.loading')}</p>}{error && <p className="alert alert-error">{error}</p>}
    {!loading && !error && result && result.items.length === 0 && <p className="muted empty-state">{t('audit.empty')}</p>}
    {!loading && !error && result && result.items.length > 0 && <>
      <div className="card table-card"><table><thead><tr><th>{t('audit.createdAt')}</th><th>{t('audit.actor')}</th><th>{t('audit.actionLabel')}</th><th>{t('audit.resourceType')}</th><th>{t('audit.resourceId')}</th><th>{t('audit.changedFields')}</th></tr></thead><tbody>{result.items.map(entry => <tr key={entry.id}><td>{new Date(entry.createdAt).toLocaleString()}</td><td>{entry.actor?.displayName ?? '—'}</td><td>{t(`audit.action.${entry.action}` as never) || entry.action}</td><td>{t(`audit.resource.${entry.resourceType}` as never) || entry.resourceType}</td><td>{entry.resourceId ?? '—'}</td><td>{entry.metadata?.changedFields?.join(', ') || '—'}</td></tr>)}</tbody></table></div>
      <div className="pagination"><button type="button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>{t('common.previous')}</button><span>{t('audit.page')} {page + 1} {t('audit.of')} {Math.max(1, result.totalPages)} ({result.total})</span><button type="button" disabled={page + 1 >= result.totalPages} onClick={() => setPage(value => value + 1)}>{t('common.next')}</button></div>
    </>}
  </section>
}
