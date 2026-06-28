import { Navigate, useLocation } from 'react-router-dom'
import useAuthStore from '../store/authStore'

function ProtectedRoute({ children, adminOnly = false }) {
  const { token, user } = useAuthStore()
  const location = useLocation()

  if (!token) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  if (adminOnly && user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return children
}

export default ProtectedRoute
