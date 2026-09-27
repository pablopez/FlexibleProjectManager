import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthProvider'
import { AppLayout } from '../../widgets/app-layout/AppLayout'
import { SetupPage } from '../../pages/setup/SetupPage'
import { LoginPage } from '../../pages/login/LoginPage'
import { DashboardPage } from '../../pages/dashboard/DashboardPage'
import { ProjectsPage } from '../../pages/projects/ProjectsPage'
import { UsersPage } from '../../pages/users/UsersPage'
import { OrganizationPage } from '../../pages/organization/OrganizationPage'
import { LicensePage } from '../../pages/license/LicensePage'
import { SettingsPage } from '../../pages/settings/SettingsPage'
import { resolveProtectedPath, resolveRootPath } from './routePolicy'

export function LoadingScreen() {
  return <main className="state-screen" aria-live="polite">Checking your session…</main>
}

export function ErrorScreen() {
  const { retry } = useAuth()
  return (
    <main className="state-screen" aria-live="assertive">
      <h1>Unable to connect to Flexible Project Manager.</h1>
      <button type="button" onClick={() => void retry()}>Try again</button>
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
        <Route path="users" element={<UsersPage />} />
        <Route path="organization" element={<OrganizationPage />} />
        <Route path="license" element={<LicensePage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  )
}
