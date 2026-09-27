import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../shared/auth/AuthProvider'
import { isNavigationItemActive, visibleNavigation } from './navigation'

export function Sidebar() {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const items = visibleNavigation(user?.roles ?? [])

  async function signOut() {
    await logout()
  }

  return (
    <>
      <button className="menu-button" type="button" aria-label="Toggle navigation" aria-expanded={open} onClick={() => setOpen((current) => !current)}>☰</button>
      <aside className={`sidebar${open ? ' sidebar-open' : ''}`}>
        <div className="brand">FPM</div>
        <nav aria-label="Application navigation">
          <ul className="nav-list">
            {items.map((item) => (
              <li key={item.path}>
                <NavLink to={item.path} className={isNavigationItemActive(pathname, item.path) ? 'nav-link nav-link-active' : 'nav-link'} onClick={() => setOpen(false)}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <button className="logout-button" type="button" onClick={() => void signOut()}>Log out</button>
      </aside>
    </>
  )
}
