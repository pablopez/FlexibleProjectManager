import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SettingsPage } from './SettingsPage'
import { UserPreferencesProvider, useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'
import { useAuth } from '../../shared/auth/AuthProvider'
import { getUserSettings, updateUserSettings } from '../../shared/api/userSettingsApi'

vi.mock('../../shared/auth/AuthProvider', () => ({ useAuth: vi.fn() }))
vi.mock('../../shared/api/userSettingsApi', () => ({ getUserSettings: vi.fn(), updateUserSettings: vi.fn() }))

const auth = vi.mocked(useAuth)
const api = { get: vi.mocked(getUserSettings), update: vi.mocked(updateUserSettings) }

function renderPage() {
  return render(<UserPreferencesProvider><SettingsPage /></UserPreferencesProvider>)
}

function PreferenceProbe() {
  const { language, theme } = useUserPreferences()
  return <output data-testid="preference-probe">{language}/{theme}</output>
}

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    auth.mockReturnValue({ status: 'authenticated', user: null } as ReturnType<typeof useAuth>)
    api.get.mockResolvedValue({ language: 'en', theme: 'light' })
    api.update.mockResolvedValue({ language: 'en', theme: 'light' })
    document.documentElement.dataset.theme = 'light'
  })
  afterEach(() => cleanup())

  it('uses English/light fallback before preferences load', () => {
    api.get.mockReturnValue(new Promise(() => undefined))
    renderPage()
    expect((screen.getByRole('combobox', { name: 'Language' }) as HTMLSelectElement).value).toBe('en')
    expect((screen.getByRole('combobox', { name: 'Theme' }) as HTMLSelectElement).value).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
  })

  it('loads and restores saved language and theme', async () => {
    api.get.mockResolvedValue({ language: 'es', theme: 'dark' })
    renderPage()
    await waitFor(() => expect((screen.getByRole('combobox', { name: 'Idioma' }) as HTMLSelectElement).value).toBe('es'))
    expect((screen.getByRole('combobox', { name: 'Tema' }) as HTMLSelectElement).value).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('saves both preferences and applies the returned values', async () => {
    const user = userEvent.setup()
    api.update.mockResolvedValue({ language: 'es', theme: 'system' })
    renderPage()
    await screen.findByRole('combobox', { name: 'Language' })
    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'es')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Theme' }), 'system')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(api.update).toHaveBeenCalledWith({ language: 'es', theme: 'system' })
    expect(await screen.findByText('Preferencias guardadas.')).toBeTruthy()
  })

  it('shows save and load failures without invalidating authentication', async () => {
    api.get.mockRejectedValue(new Error('License inactive'))
    renderPage()
    expect(await screen.findByText('License inactive')).toBeTruthy()
    expect(auth).toHaveBeenCalled()
    api.update.mockRejectedValue(new Error('Validation failed'))
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText('Validation failed')).toBeTruthy()
  })

  it('updates system theme when the media preference changes and keeps system selected', async () => {
    let listener: ((event: MediaQueryListEvent) => void) | undefined
    const remove = vi.fn()
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false, addEventListener: (_: string, callback: (event: MediaQueryListEvent) => void) => { listener = callback }, removeEventListener: remove })))
    api.get.mockResolvedValue({ language: 'en', theme: 'system' })
    renderPage()
    await waitFor(() => expect((screen.getByRole('combobox', { name: 'Theme' }) as HTMLSelectElement).value).toBe('system'))
    expect(document.documentElement.dataset.theme).toBe('light')
    listener?.({ matches: true } as MediaQueryListEvent)
    await waitFor(() => expect(document.documentElement.dataset.theme).toBe('dark'))
    expect((screen.getByRole('combobox', { name: 'Theme' }) as HTMLSelectElement).value).toBe('system')
    api.update.mockResolvedValue({ language: 'en', theme: 'light' })
    await userEvent.setup().click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(remove).toHaveBeenCalled())
  })

  it('ignores a preference load that resolves after logout', async () => {
    let resolveLoad!: (value: { language: 'es'; theme: 'dark' }) => void
    api.get.mockReturnValueOnce(new Promise(resolve => { resolveLoad = resolve }))
    const view = render(<UserPreferencesProvider><SettingsPage /></UserPreferencesProvider>)
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))

    auth.mockReturnValue({ status: 'unauthenticated', user: null } as ReturnType<typeof useAuth>)
    view.rerender(<UserPreferencesProvider><SettingsPage /></UserPreferencesProvider>)
    resolveLoad({ language: 'es', theme: 'dark' })

    await waitFor(() => expect((screen.getByRole('combobox', { name: 'Language' }) as HTMLSelectElement).value).toBe('en'))
    expect((screen.getByRole('combobox', { name: 'Theme' }) as HTMLSelectElement).value).toBe('light')
  })

  it('does not let User A load overwrite User B preferences', async () => {
    let resolveA!: (value: { language: 'es'; theme: 'dark' }) => void
    let resolveB!: (value: { language: 'en'; theme: 'light' }) => void
    api.get.mockReturnValueOnce(new Promise(resolve => { resolveA = resolve }))
      .mockReturnValueOnce(new Promise(resolve => { resolveB = resolve }))
    const view = render(<UserPreferencesProvider><SettingsPage /><PreferenceProbe /></UserPreferencesProvider>)
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(1))

    auth.mockReturnValue({ status: 'authenticated', user: { id: 'user-b' } } as ReturnType<typeof useAuth>)
    view.rerender(<UserPreferencesProvider><SettingsPage /><PreferenceProbe /></UserPreferencesProvider>)
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2))
    resolveA({ language: 'es', theme: 'dark' })
    await waitFor(() => expect((screen.getByRole('combobox', { name: 'Language' }) as HTMLSelectElement).value).toBe('en'))
    resolveB({ language: 'en', theme: 'light' })
    expect((screen.getByRole('combobox', { name: 'Theme' }) as HTMLSelectElement).value).toBe('light')
  })

  it('ignores a PATCH response after the authenticated session changes', async () => {
    let resolveUpdate!: (value: { language: 'es'; theme: 'dark' }) => void
    api.update.mockReturnValueOnce(new Promise(resolve => { resolveUpdate = resolve }))
    const view = render(<UserPreferencesProvider><SettingsPage /><PreferenceProbe /></UserPreferencesProvider>)
    await screen.findByRole('combobox', { name: 'Language' })
    const user = userEvent.setup()
    await user.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'es')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    auth.mockReturnValue({ status: 'unauthenticated', user: null } as ReturnType<typeof useAuth>)
    view.rerender(<UserPreferencesProvider><SettingsPage /><PreferenceProbe /></UserPreferencesProvider>)
    await waitFor(() => expect(screen.getByTestId('preference-probe').textContent).toBe('en/light'))
    resolveUpdate({ language: 'es', theme: 'dark' })

    await waitFor(() => expect(screen.getByTestId('preference-probe').textContent).toBe('en/light'))
    expect(screen.queryByText('Preferencias guardadas.')).toBeNull()
  })
})
