import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { getUserSettings, updateUserSettings, type UserSettings, type UserSettingsPatch } from '../api/userSettingsApi'
import { useAuth } from '../auth/AuthProvider'

export const DEFAULT_USER_SETTINGS: UserSettings = { language: 'en', theme: 'light' }
type TranslationKey = keyof typeof translations.en

const translations = {
  en: {
    'common.save': 'Save changes', 'common.saving': 'Saving…', 'common.cancel': 'Cancel', 'common.previous': 'Previous', 'common.next': 'Next', 'common.retry': 'Retry', 'common.tryAgain': 'Try again', 'common.connectionError': 'Unable to connect to Flexible Project Manager.',
    'common.loading': 'Loading…', 'common.forbidden': 'Forbidden', 'common.none': 'None', 'common.all': 'All', 'common.active': 'Active', 'common.disabled': 'Disabled',
    'navigation.dashboard': 'Dashboard', 'navigation.projects': 'Projects', 'navigation.users': 'Users', 'navigation.organization': 'Organization', 'navigation.license': 'License', 'navigation.settings': 'Settings', 'navigation.logout': 'Log out', 'navigation.toggle': 'Toggle navigation', 'navigation.aria': 'Application navigation',
    'header.currentUser': 'Current user',
    'auth.signIn': 'Sign in', 'auth.continue': 'Use your platform account to continue.', 'auth.signingIn': 'Signing in…', 'auth.email': 'Email', 'auth.password': 'Password', 'auth.failed': 'Authentication failed.',
    'setup.eyebrow': 'First-run setup', 'setup.title': 'Initialize Flexible Project Manager', 'setup.description': 'Create the first organization and administrator account.', 'setup.organization': 'Organization name', 'setup.installation': 'Installation name', 'setup.adminEmail': 'Administrator email', 'setup.adminDisplayName': 'Administrator display name', 'setup.adminPassword': 'Administrator password', 'setup.initialize': 'Initialize installation', 'setup.initializing': 'Initializing…', 'setup.failed': 'Setup failed.',
    'dashboard.title': 'Dashboard', 'dashboard.welcome': 'Welcome', 'dashboard.ready': 'Flexible Project Manager is ready.',
    'projects.eyebrow': 'Platform Core', 'projects.title': 'Projects', 'projects.description': 'Generic projects identified by a stable platform ID.', 'projects.new': 'New project', 'projects.loading': 'Loading projects…', 'projects.empty': 'No projects found.', 'projects.name': 'Name', 'projects.descriptionField': 'Description', 'projects.status': 'Status', 'projects.newTitle': 'New project', 'projects.projectName': 'Project name', 'projects.create': 'Create project', 'projects.creating': 'Creating…', 'projects.back': 'Back to projects', 'projects.project': 'Project', 'projects.save': 'Save changes', 'projects.archive': 'Archive project', 'projects.restore': 'Restore project', 'projects.loadingOne': 'Loading project…', 'projects.notFound': 'Project not found.', 'projects.loadError': 'Unable to load project.', 'projects.updateError': 'Unable to update project.', 'projects.statusError': 'Unable to change project status.', 'projects.permissionCreate': 'You do not have permission to create projects.', 'projects.page': 'Page', 'projects.of': 'of',
    'users.eyebrow': 'Access control', 'users.title': 'Users', 'users.description': 'Manage members of this organization.', 'users.new': 'New user', 'users.loading': 'Loading users…', 'users.empty': 'No users found.', 'users.name': 'Name', 'users.email': 'Email', 'users.status': 'Status', 'users.roles': 'Roles', 'users.newTitle': 'New user', 'users.displayName': 'Display name', 'users.password': 'Password', 'users.loadingRoles': 'Loading roles…', 'users.create': 'Create user', 'users.creating': 'Creating…', 'users.back': 'Back to users', 'users.loadingOne': 'Loading user…', 'users.notFound': 'User not found.', 'users.loadError': 'Unable to load users.', 'users.createError': 'Unable to create user.', 'users.updateError': 'Unable to update user.', 'users.statusError': 'Unable to update status.', 'users.rolesError': 'Unable to update roles.', 'users.saveDisplayName': 'Save display name', 'users.saveRoles': 'Save roles', 'users.disable': 'Disable', 'users.activate': 'Activate', 'users.permissionView': 'You do not have permission to view users.', 'users.permissionCreate': 'You do not have permission to create users.', 'users.page': 'Page', 'users.of': 'of',
    'organization.eyebrow': 'Platform Core', 'organization.title': 'Organization', 'organization.description': 'Manage the organization associated with this local installation.', 'organization.organization': 'Organization', 'organization.installation': 'Installation', 'organization.name': 'Name', 'organization.slug': 'Slug', 'organization.identifier': 'Identifier', 'organization.platform': 'Platform', 'organization.version': 'Version', 'organization.created': 'Created', 'organization.updated': 'Updated', 'organization.lastSeen': 'Last seen', 'organization.loading': 'Loading organization…', 'organization.loadError': 'Unable to load organization.', 'organization.saveError': 'Unable to update organization.', 'organization.save': 'Save changes', 'organization.saving': 'Saving…', 'organization.permission': 'You do not have permission to view the organization.',
    'license.eyebrow': 'Platform Core', 'license.title': 'License', 'license.description': 'Manage the signed license for this installation.', 'license.loading': 'Loading license…', 'license.loadError': 'Unable to load license information.', 'license.activateError': 'Unable to activate the license.', 'license.deactivateError': 'Unable to deactivate the license.', 'license.status': 'License status', 'license.installationId': 'Installation ID', 'license.licenseId': 'License ID', 'license.type': 'Type', 'license.issued': 'Issued', 'license.expires': 'Expires', 'license.perpetual': 'Perpetual', 'license.maxUsers': 'Max users', 'license.features': 'License features', 'license.activateOrReplace': 'Activate or replace', 'license.signed': 'Signed license', 'license.activate': 'Activate license', 'license.deactivate': 'Deactivate license', 'license.permission': 'You do not have permission to view licensing.', 'license.unlicensed': 'UNLICENSED',
    'settings.eyebrow': 'Preferences', 'settings.title': 'Settings', 'settings.description': 'Customize your language and theme.', 'settings.language': 'Language', 'settings.english': 'English', 'settings.spanish': 'Español', 'settings.theme': 'Theme', 'settings.light': 'Light', 'settings.dark': 'Dark', 'settings.system': 'System', 'settings.loadError': 'Unable to load your preferences.', 'settings.saveError': 'Unable to save your preferences.', 'settings.saved': 'Preferences saved.',
  },
  es: {
    'common.save': 'Guardar cambios', 'common.saving': 'Guardando…', 'common.cancel': 'Cancelar', 'common.previous': 'Anterior', 'common.next': 'Siguiente', 'common.retry': 'Reintentar', 'common.tryAgain': 'Intentar de nuevo', 'common.connectionError': 'No se pudo conectar con Flexible Project Manager.', 'common.loading': 'Cargando…', 'common.forbidden': 'Prohibido', 'common.none': 'Ninguno', 'common.all': 'Todos', 'common.active': 'Activo', 'common.disabled': 'Deshabilitado',
    'navigation.dashboard': 'Panel', 'navigation.projects': 'Proyectos', 'navigation.users': 'Usuarios', 'navigation.organization': 'Organización', 'navigation.license': 'Licencia', 'navigation.settings': 'Configuración', 'navigation.logout': 'Cerrar sesión', 'navigation.toggle': 'Mostrar navegación', 'navigation.aria': 'Navegación de la aplicación',
    'header.currentUser': 'Usuario actual', 'auth.signIn': 'Iniciar sesión', 'auth.continue': 'Usa tu cuenta de la plataforma para continuar.', 'auth.signingIn': 'Iniciando sesión…', 'auth.email': 'Correo electrónico', 'auth.password': 'Contraseña', 'auth.failed': 'La autenticación falló.',
    'setup.eyebrow': 'Configuración inicial', 'setup.title': 'Inicializar Flexible Project Manager', 'setup.description': 'Crea la primera organización y cuenta de administrador.', 'setup.organization': 'Nombre de la organización', 'setup.installation': 'Nombre de la instalación', 'setup.adminEmail': 'Correo del administrador', 'setup.adminDisplayName': 'Nombre del administrador', 'setup.adminPassword': 'Contraseña del administrador', 'setup.initialize': 'Inicializar instalación', 'setup.initializing': 'Inicializando…', 'setup.failed': 'La configuración falló.',
    'dashboard.title': 'Panel', 'dashboard.welcome': 'Bienvenido', 'dashboard.ready': 'Flexible Project Manager está listo.',
    'projects.eyebrow': 'Núcleo de la plataforma', 'projects.title': 'Proyectos', 'projects.description': 'Proyectos genéricos identificados por un ID estable.', 'projects.new': 'Nuevo proyecto', 'projects.loading': 'Cargando proyectos…', 'projects.empty': 'No se encontraron proyectos.', 'projects.name': 'Nombre', 'projects.descriptionField': 'Descripción', 'projects.status': 'Estado', 'projects.newTitle': 'Nuevo proyecto', 'projects.projectName': 'Nombre del proyecto', 'projects.create': 'Crear proyecto', 'projects.creating': 'Creando…', 'projects.back': 'Volver a proyectos', 'projects.project': 'Proyecto', 'projects.save': 'Guardar cambios', 'projects.archive': 'Archivar proyecto', 'projects.restore': 'Restaurar proyecto', 'projects.loadingOne': 'Cargando proyecto…', 'projects.notFound': 'Proyecto no encontrado.', 'projects.loadError': 'No se pudo cargar el proyecto.', 'projects.updateError': 'No se pudo actualizar el proyecto.', 'projects.statusError': 'No se pudo cambiar el estado del proyecto.', 'projects.permissionCreate': 'No tienes permiso para crear proyectos.', 'projects.page': 'Página', 'projects.of': 'de',
    'users.eyebrow': 'Control de acceso', 'users.title': 'Usuarios', 'users.description': 'Administra los miembros de esta organización.', 'users.new': 'Nuevo usuario', 'users.loading': 'Cargando usuarios…', 'users.empty': 'No se encontraron usuarios.', 'users.name': 'Nombre', 'users.email': 'Correo electrónico', 'users.status': 'Estado', 'users.roles': 'Roles', 'users.newTitle': 'Nuevo usuario', 'users.displayName': 'Nombre visible', 'users.password': 'Contraseña', 'users.loadingRoles': 'Cargando roles…', 'users.create': 'Crear usuario', 'users.creating': 'Creando…', 'users.back': 'Volver a usuarios', 'users.loadingOne': 'Cargando usuario…', 'users.notFound': 'Usuario no encontrado.', 'users.loadError': 'No se pudieron cargar los usuarios.', 'users.createError': 'No se pudo crear el usuario.', 'users.updateError': 'No se pudo actualizar el usuario.', 'users.statusError': 'No se pudo actualizar el estado.', 'users.rolesError': 'No se pudieron actualizar los roles.', 'users.saveDisplayName': 'Guardar nombre', 'users.saveRoles': 'Guardar roles', 'users.disable': 'Deshabilitar', 'users.activate': 'Activar', 'users.permissionView': 'No tienes permiso para ver usuarios.', 'users.permissionCreate': 'No tienes permiso para crear usuarios.', 'users.page': 'Página', 'users.of': 'de',
    'organization.eyebrow': 'Núcleo de la plataforma', 'organization.title': 'Organización', 'organization.description': 'Administra la organización asociada a esta instalación local.', 'organization.organization': 'Organización', 'organization.installation': 'Instalación', 'organization.name': 'Nombre', 'organization.slug': 'Slug', 'organization.identifier': 'Identificador', 'organization.platform': 'Plataforma', 'organization.version': 'Versión', 'organization.created': 'Creada', 'organization.updated': 'Actualizada', 'organization.lastSeen': 'Última actividad', 'organization.loading': 'Cargando organización…', 'organization.loadError': 'No se pudo cargar la organización.', 'organization.saveError': 'No se pudo actualizar la organización.', 'organization.save': 'Guardar cambios', 'organization.saving': 'Guardando…', 'organization.permission': 'No tienes permiso para ver la organización.',
    'license.eyebrow': 'Núcleo de la plataforma', 'license.title': 'Licencia', 'license.description': 'Administra la licencia firmada de esta instalación.', 'license.loading': 'Cargando licencia…', 'license.loadError': 'No se pudo cargar la información de la licencia.', 'license.activateError': 'No se pudo activar la licencia.', 'license.deactivateError': 'No se pudo desactivar la licencia.', 'license.status': 'Estado de la licencia', 'license.installationId': 'ID de instalación', 'license.licenseId': 'ID de licencia', 'license.type': 'Tipo', 'license.issued': 'Emitida', 'license.expires': 'Caduca', 'license.perpetual': 'Perpetua', 'license.maxUsers': 'Usuarios máximos', 'license.features': 'Funciones de la licencia', 'license.activateOrReplace': 'Activar o reemplazar', 'license.signed': 'Licencia firmada', 'license.activate': 'Activar licencia', 'license.deactivate': 'Desactivar licencia', 'license.permission': 'No tienes permiso para ver las licencias.', 'license.unlicensed': 'SIN LICENCIA',
    'settings.eyebrow': 'Preferencias', 'settings.title': 'Configuración', 'settings.description': 'Personaliza tu idioma y tema.', 'settings.language': 'Idioma', 'settings.english': 'English', 'settings.spanish': 'Español', 'settings.theme': 'Tema', 'settings.light': 'Claro', 'settings.dark': 'Oscuro', 'settings.system': 'Sistema', 'settings.loadError': 'No se pudieron cargar tus preferencias.', 'settings.saveError': 'No se pudieron guardar tus preferencias.', 'settings.saved': 'Preferencias guardadas.',
  },
} as const

type PreferencesContextValue = {
  language: UserSettings['language']; theme: UserSettings['theme']; effectiveTheme: 'light' | 'dark'; loading: boolean; error: string | null
  t: (key: TranslationKey) => string; update: (patch: UserSettingsPatch) => Promise<UserSettings | null>; reload: () => Promise<void>
}

const PreferencesContext = createContext<PreferencesContextValue | null>(null)

export function UserPreferencesProvider({ children }: { children: ReactNode }) {
  const { status, user } = useAuth()
  const [settings, setSettings] = useState<UserSettings>(DEFAULT_USER_SETTINGS)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [systemDark, setSystemDark] = useState(false)
  const sessionGeneration = useRef(0)
  const requestSequence = useRef(0)
  const sessionKey = status === 'authenticated' ? user?.id ?? 'authenticated' : null

  const load = useCallback(async () => {
    if (!sessionKey) return
    const owner = { session: sessionKey, generation: sessionGeneration.current, request: ++requestSequence.current }
    const isCurrent = () => owner.session === sessionKey && owner.generation === sessionGeneration.current && owner.request === requestSequence.current
    setLoading(true); setError(null)
    try {
      const loaded = await getUserSettings()
      if (isCurrent()) setSettings(loaded)
    } catch (cause) {
      if (isCurrent()) { setSettings(DEFAULT_USER_SETTINGS); setError(cause instanceof Error ? cause.message : 'Unable to load your preferences.') }
    } finally {
      if (isCurrent()) setLoading(false)
    }
  }, [sessionKey])

  useEffect(() => {
    sessionGeneration.current += 1
    requestSequence.current += 1
    setSettings(DEFAULT_USER_SETTINGS)
    setError(null)
    setLoading(false)
    if (sessionKey) void load()
  }, [load, sessionKey])
  useEffect(() => {
    if (settings.theme !== 'system' || typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    const query = window.matchMedia('(prefers-color-scheme: dark)')
    const changed = (event?: MediaQueryListEvent) => setSystemDark(event?.matches ?? query.matches)
    changed(); query.addEventListener?.('change', changed)
    return () => query.removeEventListener?.('change', changed)
  }, [settings.theme])
  const effectiveTheme = settings.theme === 'system' ? (systemDark ? 'dark' : 'light') : settings.theme
  useEffect(() => { if (typeof document !== 'undefined') document.documentElement.dataset.theme = effectiveTheme }, [effectiveTheme])

  const update = useCallback(async (patch: UserSettingsPatch): Promise<UserSettings | null> => {
    if (!sessionKey) return null
    const owner = { session: sessionKey, generation: sessionGeneration.current, request: ++requestSequence.current }
    const isCurrent = () => owner.session === sessionKey && owner.generation === sessionGeneration.current && owner.request === requestSequence.current
    try {
      const saved = await updateUserSettings(patch)
      if (!isCurrent()) return null
      setSettings(saved); setError(null); return saved
    } catch (cause) {
      if (!isCurrent()) return null
      throw cause
    }
  }, [sessionKey])
  const value = useMemo(() => ({ language: settings.language, theme: settings.theme, effectiveTheme, loading, error, t: (key: TranslationKey) => translations[settings.language][key] ?? translations.en[key], update, reload: load }), [effectiveTheme, error, load, loading, settings.language, settings.theme, update])
  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>
}

export function useUserPreferences(): PreferencesContextValue {
  const value = useContext(PreferencesContext)
  if (value) return value
  return {
    language: DEFAULT_USER_SETTINGS.language,
    theme: DEFAULT_USER_SETTINGS.theme,
    effectiveTheme: 'light',
    loading: false,
    error: null,
    t: (key: TranslationKey) => translations.en[key],
    update: async patch => ({ ...DEFAULT_USER_SETTINGS, ...patch }),
    reload: async () => undefined,
  }
}
