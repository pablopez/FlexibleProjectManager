import { BrowserRouter } from 'react-router-dom'
import { AppRouter } from './router/AppRouter'
import { AuthProvider } from '../shared/auth/AuthProvider'
import { UserPreferencesProvider } from '../shared/preferences/UserPreferencesProvider'
import './app.css'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <UserPreferencesProvider>
          <AppRouter />
        </UserPreferencesProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
