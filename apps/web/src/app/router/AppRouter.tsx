import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthProvider'
import { AppLayout } from '../../widgets/app-layout/AppLayout'
import { SetupPage } from '../../pages/setup/SetupPage'
import { LoginPage } from '../../pages/login/LoginPage'
import { DashboardPage } from '../../pages/dashboard/DashboardPage'
import { ProjectsPage } from '../../pages/projects/ProjectsPage'
import { CreateProjectPage } from '../../pages/projects/CreateProjectPage'
import { ProjectDetailPage } from '../../pages/projects/ProjectDetailPage'
import { UsersPage } from '../../pages/users/UsersPage'
import { CreateUserPage } from '../../pages/users/CreateUserPage'
import { UserDetailPage } from '../../pages/users/UserDetailPage'
import { OrganizationPage } from '../../pages/organization/OrganizationPage'
import { LicensePage } from '../../pages/license/LicensePage'
import { SettingsPage } from '../../pages/settings/SettingsPage'
import { AuditPage } from '../../pages/audit/AuditPage'
import { resolveProtectedPath, resolveRootPath } from './routePolicy'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function LoadingScreen() {
  return <main className="state-screen" aria-live="polite">Checking your session…</main>
}

export function ErrorScreen() {
  const { retry } = useAuth()
  const { t } = useUserPreferences()
  return (
    <main className="state-screen" aria-live="assertive">
      <h1>{t('common.connectionError')}</h1>
      <button type="button" onClick={() => void retry()}>{t('common.tryAgain')}</button>
    </main>
  )
}

function SetupGuard() {
  const { status } = useAuth()
  if (status === 'loading') return <LoadingScreen />
  if (status === 'error') return <ErrorScreen />
  if (status !== 'setup-required') return <Navigate to={status === 'authenticated' ? '/app/dashboard' : '/login'} replace />
  return <Outlet />
}

function LoginGuard() {
  const { status } = useAuth()
  if (status === 'loading') return <LoadingScreen />
  if (status === 'error') return <ErrorScreen />
  if (status === 'setup-required') return <Navigate to="/setup" replace />
  if (status === 'authenticated') return <Navigate to="/app/dashboard" replace />
  return <Outlet />
}

function ProtectedApp() {
  const { status } = useAuth()
  if (status === 'error') return <ErrorScreen />
  const destination = resolveProtectedPath(status)
  if (destination) return <Navigate to={destination} replace />
  if (status === 'loading') return <LoadingScreen />
  return <AppLayout />
}

function RootRedirect() {
  const { status } = useAuth()
  if (status === 'error') return <ErrorScreen />
  const destination = resolveRootPath(status)
  return destination ? <Navigate to={destination} replace /> : <LoadingScreen />
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route element={<SetupGuard />}>
        <Route path="/setup" element={<SetupPage />} />
      </Route>
      <Route element={<LoginGuard />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>
      <Route path="/app" element={<ProtectedApp />}>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="projects/new" element={<CreateProjectPage />} />
        <Route path="projects/:projectId" element={<ProjectDetailPage />} />
         <Route path="users" element={<UsersPage />} />
         <Route path="users/new" element={<CreateUserPage />} />
         <Route path="users/:userId" element={<UserDetailPage />} />
        <Route path="organization" element={<OrganizationPage />} />
        <Route path="license" element={<LicensePage />} />
         <Route path="settings" element={<SettingsPage />} />
         <Route path="audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  )
}
