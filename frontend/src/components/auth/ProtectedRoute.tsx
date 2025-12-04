import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

interface ProtectedRouteProps {
  children: ReactNode
}

/**
 * ProtectedRoute component that redirects to login if authentication is required
 * and the user is not authenticated.
 *
 * This component is used to protect routes that allow write operations (POST/PUT/DELETE).
 */
export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { user, isLoading, authRequired } = useAuth()
  const location = useLocation()

  // Wait for auth check to complete
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    )
  }

  // If auth is required and user is not authenticated, redirect to login
  if (authRequired && !user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  // Otherwise, render the protected content
  return <>{children}</>
}
