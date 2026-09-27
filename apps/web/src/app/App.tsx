import { BrowserRouter } from 'react-router-dom'
import { AppRouter } from './router/AppRouter'
import { AuthProvider } from '../shared/auth/AuthProvider'
import './app.css'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRouter />
      </AuthProvider>
    </BrowserRouter>
  )
}
