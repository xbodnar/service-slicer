import { Outlet, Link, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { Layers, FlaskConical, Scissors, Github, Menu, X, FileArchive, Server, Settings } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { ScrollProgress } from './ScrollProgress'
import { useState } from 'react'

export function MainLayout() {
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const isActive = (path: string) => {
    return location.pathname.startsWith(path)
  }

  const navItems = [
    { path: '/projects', label: 'Projects', icon: Layers },
    { path: '/benchmarks', label: 'Benchmarks', icon: FlaskConical },
    { path: '/operational-settings', label: 'Operational Settings', icon: Settings },
    { path: '/systems-under-test', label: 'Systems Under Test', icon: Server },
    { path: '/files', label: 'Files', icon: FileArchive },
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20">
      {/* Scroll Progress Bar */}
      <ScrollProgress />

      {/* Mobile Header */}
      <header className="lg:hidden sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 shadow-sm">
        <div className="flex items-center justify-between h-16 px-4">
          <Link to="/" className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Scissors className="h-5 w-5 text-primary" />
            </div>
            <div className="flex flex-col">
              <span className="text-lg font-bold tracking-tight">ServiceSlicer</span>
            </div>
          </Link>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label="Toggle menu"
          >
            {sidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </Button>
        </div>
      </header>

      <div className="flex h-screen lg:h-auto">
        {/* Sidebar */}
        <aside
          className={cn(
            'fixed lg:sticky top-0 left-0 z-40 h-screen w-64 border-r bg-background/95 backdrop-blur transition-transform duration-300 ease-in-out lg:translate-x-0 flex flex-col',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full'
          )}
        >
          {/* Logo Section */}
          <div className="p-6 border-b">
            <Link to="/" className="flex items-center gap-3 group" onClick={() => setSidebarOpen(false)}>
              <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary/20 to-primary/10 group-hover:from-primary/30 group-hover:to-primary/20 transition-all shadow-sm">
                <Scissors className="h-6 w-6 text-primary" />
              </div>
              <div className="flex flex-col">
                <span className="text-xl font-bold tracking-tight">ServiceSlicer</span>
                <span className="text-xs text-muted-foreground">Microservice Analysis</span>
              </div>
            </Link>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-2">
            <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3 px-3">
              Main
            </div>
            {navItems.map((item) => {
              const Icon = item.icon
              const active = isActive(item.path)
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  className={cn(
                    'group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all relative',
                    active
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                  )}
                >
                  {active && (
                    <div className="absolute left-0 w-1 h-8 bg-primary-foreground rounded-r-full" />
                  )}
                  <Icon className={cn('h-5 w-5 flex-shrink-0', active ? '' : 'group-hover:scale-110 transition-transform')} />
                  <span>{item.label}</span>
                </Link>
              )
            })}
          </nav>

          {/* Footer Section */}
          <div className="p-4 border-t space-y-3">
            <Separator />
            <a
              href="https://github.com/xbodnar/service-slicer"
              target="_blank"
              rel="noopener noreferrer"
              className="block"
            >
              <Button variant="outline" size="sm" className="w-full justify-start gap-2 hover:bg-primary/10">
                <Github className="h-4 w-4" />
                <span>View on GitHub</span>
              </Button>
            </a>
            <div className="flex items-center justify-between px-3 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5">
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                <span>Online</span>
              </div>
              <span>v0.1.0</span>
            </div>
          </div>
        </aside>

        {/* Mobile Overlay */}
        {sidebarOpen && (
          <div
            className="fixed inset-0 bg-background/80 backdrop-blur-sm z-30 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        {/* Main Content */}
        <div className="flex-1 flex flex-col min-h-screen lg:min-h-0">
          <main className="flex-1 overflow-auto">
            <div className="container mx-auto px-4 lg:px-8 py-6 lg:py-8 max-w-[1600px]">
              <Outlet />
            </div>
          </main>

          {/* Footer */}
          <footer className="border-t bg-background/50 backdrop-blur mt-auto">
            <div className="container mx-auto px-4 lg:px-8 py-4">
              <div className="flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-muted-foreground">
                <div className="flex items-center gap-2">
                  <Scissors className="h-3.5 w-3.5" />
                  <span>© {new Date().getFullYear()} ServiceSlicer</span>
                  <span className="hidden sm:inline">•</span>
                  <span className="hidden sm:inline">Microservice Decomposition Tool</span>
                </div>
                <div>Thesis Project by Norbert Bodnar</div>
              </div>
            </div>
          </footer>
        </div>
      </div>
    </div>
  )
}
