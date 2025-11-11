import { Outlet, Link, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { Layers, FlaskConical } from 'lucide-react'

export function MainLayout() {
  const location = useLocation()

  const isActive = (path: string) => {
    return location.pathname.startsWith(path)
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="border-b bg-white">
        <div className="container mx-auto px-4">
          <div className="flex items-center h-16">
            <Link to="/" className="text-xl font-bold mr-8">
              ServiceSlicer
            </Link>
            <nav className="flex gap-6">
              <Link
                to="/projects"
                className={cn(
                  'flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/projects')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                )}
              >
                <Layers className="h-4 w-4" />
                Projects
              </Link>
              <Link
                to="/experiments"
                className={cn(
                  'flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors',
                  isActive('/experiments')
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                )}
              >
                <FlaskConical className="h-4 w-4" />
                Experiments
              </Link>
            </nav>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 bg-muted/10">
        <div className="container mx-auto px-4 py-8">
          <Outlet />
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t py-4 bg-white">
        <div className="container mx-auto px-4 text-center text-sm text-muted-foreground">
          ServiceSlicer &copy; {new Date().getFullYear()}
        </div>
      </footer>
    </div>
  )
}
