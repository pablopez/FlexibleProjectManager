import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthProvider'
import { isNavigationItemActive, visibleNavigation } from './navigation'
import { useUserPreferences } from '../../shared/preferences/UserPreferencesProvider'

export function Sidebar() {
  const { user, logout } = useAuth()
  const { t } = useUserPreferences()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const items = visibleNavigation(user?.roles ?? [], user?.permissions ?? [])

  async function signOut() {
    try {
      await logout()
    } catch {
      // AuthProvider clears the local session in its finally block.
    }
  }

  return (
    <>
       <button className="menu-button" type="button" aria-label={t('navigation.toggle')} aria-expanded={open} onClick={() => setOpen((current) => !current)}>☰</button>
      <aside className={`sidebar${open ? ' sidebar-open' : ''}`}>
        <div className="brand">FPM</div>
        <nav aria-label={t('navigation.aria')}>
          <ul className="nav-list">
            {items.map((item) => (
              <li key={item.path}>
                <NavLink to={item.path} className={isNavigationItemActive(pathname, item.path) ? 'nav-link nav-link-active' : 'nav-link'} onClick={() => setOpen(false)}>
                   {t(`navigation.${item.key}` as 'navigation.dashboard')}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
         <button className="logout-button" type="button" onClick={() => void signOut()}>{t('navigation.logout')}</button>
      </aside>
    </>
  )
}
