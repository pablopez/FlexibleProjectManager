import { useEffect, useState } from 'react'
import type { UserSettings } from '../../shared/api/userSettingsApi'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function SettingsPage() {
  const { language, theme, loading, error: loadError, t, update, reload } = useUserPreferences()
  const [selectedLanguage, setSelectedLanguage] = useState<UserSettings['language']>(language)
  const [selectedTheme, setSelectedTheme] = useState<UserSettings['theme']>(theme)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => { setSelectedLanguage(language); setSelectedTheme(theme) }, [language, theme])

  async function save() {
    setSaving(true); setError(null); setSaved(false)
    try { if (await update({ language: selectedLanguage, theme: selectedTheme })) setSaved(true) }
    catch (cause) { setError(cause instanceof Error ? cause.message : t('settings.saveError')) }
    finally { setSaving(false) }
  }

  return <section className="page-content">
    <div className="page-heading"><div><p className="eyebrow">{t('settings.eyebrow')}</p><h1>{t('settings.title')}</h1><p className="muted">{t('settings.description')}</p></div></div>
    {loadError && <div className="alert alert-error" role="alert"><p>{loadError}</p><button type="button" onClick={() => void reload()}>{t('common.retry')}</button></div>}
    {error && <p className="alert alert-error" role="alert">{error}</p>}
    {saved && <p className="alert alert-success" role="status">{t('settings.saved')}</p>}
    {loading && <p className="muted" aria-live="polite">{t('common.loading')}</p>}
    <div className="card stack-form settings-form">
      <label>{t('settings.language')}
        <select value={selectedLanguage} onChange={event => setSelectedLanguage(event.target.value as UserSettings['language'])}>
          <option value="en">{t('settings.english')}</option><option value="es">{t('settings.spanish')}</option>
        </select>
      </label>
      <label>{t('settings.theme')}
        <select value={selectedTheme} onChange={event => setSelectedTheme(event.target.value as UserSettings['theme'])}>
          <option value="light">{t('settings.light')}</option><option value="dark">{t('settings.dark')}</option><option value="system">{t('settings.system')}</option>
        </select>
      </label>
      <button type="button" disabled={saving} onClick={() => void save()}>{saving ? t('common.saving') : t('common.save')}</button>
    </div>
  </section>
}
