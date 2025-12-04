import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { axiosInstance } from '@/api/client'

interface AuthUser {
  username: string
  authenticated: boolean
}

interface AuthContextType {
  user: AuthUser | null
  isLoading: boolean
  authRequired: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  checkAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [authRequired, setAuthRequired] = useState(false)

  const checkAuth = async () => {
    try {
      const response = await axiosInstance.get<AuthUser>('/auth/status')
      // 200 response: auth is enabled and user is authenticated
      setUser(response.data)
      setAuthRequired(true)
    } catch (error: any) {
      setUser(null)
      // 404: auth endpoint doesn't exist, auth is disabled
      // 401: auth is enabled but user is not authenticated
      if (error.response?.status === 404) {
        setAuthRequired(false)
      } else if (error.response?.status === 401) {
        setAuthRequired(true)
      } else {
        // For other errors, assume auth is not required to avoid blocking users
        setAuthRequired(false)
      }
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (username: string, password: string) => {
    const response = await axiosInstance.post<AuthUser>('/auth/login', {
      username,
      password,
    })
    setUser(response.data)
    setAuthRequired(true)
  }

  const logout = async () => {
    await axiosInstance.post('/auth/logout')
    setUser(null)
  }

  useEffect(() => {
    checkAuth()
  }, [])

  return (
    <AuthContext.Provider value={{ user, isLoading, authRequired, login, logout, checkAuth }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
