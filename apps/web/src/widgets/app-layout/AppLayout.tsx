import { Outlet } from 'react-router-dom'
import { Header } from '../header/Header'
import { Sidebar } from '../sidebar/Sidebar'

export function AppLayout() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Header />
        <main className="content" aria-live="polite"><Outlet /></main>
      </div>
    </div>
  )
}
