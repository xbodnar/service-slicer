import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  ArrowRight,
  Scissors,
  FolderKanban,
  FlaskConical,
  GitBranch,
  BarChart3,
  Sparkles,
  CheckCircle2
} from 'lucide-react'

export function LandingPage() {
  const features = [
    {
      icon: GitBranch,
      title: 'Dependency Analysis',
      description: 'Analyze Java applications to discover dependencies between classes and packages',
    },
    {
      icon: Sparkles,
      title: 'Smart Decomposition',
      description: 'Multiple algorithms suggest optimal microservice boundaries based on static analysis',
    },
    {
      icon: BarChart3,
      title: 'Performance Testing',
      description: 'Compare load test results across different architectural decompositions',
    },
  ]

  const steps = [
    {
      number: '01',
      title: 'Create a Project',
      description: 'Upload your Java application JAR file and source code',
      icon: FolderKanban,
    },
    {
      number: '02',
      title: 'Analyze Dependencies',
      description: 'The system builds a dependency graph and suggests microservice boundaries',
      icon: GitBranch,
    },
    {
      number: '03',
      title: 'Run Benchmarks',
      description: 'Create load test benchmarks to compare different decomposition strategies',
      icon: FlaskConical,
    },
  ]

  return (
    <div className="space-y-16 pb-16">
      {/* Hero Section */}
      <section className="text-center space-y-6 pt-8">
        <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full bg-primary/10 border border-primary/20">
          <Scissors className="h-5 w-5 text-primary" />
          <span className="text-sm font-medium text-primary">Microservice Decomposition Analysis</span>
        </div>

        <div className="space-y-4 max-w-3xl mx-auto">
          <h1 className="text-5xl font-bold tracking-tight bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent">
            Transform Your Monolith into Microservices
          </h1>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            ServiceSlicer analyzes your Java applications and suggests optimal microservice boundaries
            using static code analysis, dependency graphs, and performance testing.
          </p>
        </div>

        <div className="flex items-center justify-center gap-4 pt-4">
          <Link to="/projects/new">
            <Button size="lg" className="gap-2 shadow-lg shadow-primary/25">
              <FolderKanban className="h-5 w-5" />
              Create Project
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
          <Link to="/projects">
            <Button variant="outline" size="lg" className="gap-2">
              View Projects
            </Button>
          </Link>
        </div>
      </section>

      {/* Features Grid */}
      <section className="space-y-8">
        <div className="text-center space-y-2">
          <h2 className="text-3xl font-bold">Key Features</h2>
          <p className="text-muted-foreground">
            Everything you need to analyze and decompose monolithic applications
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {features.map((feature) => {
            const Icon = feature.icon
            return (
              <Card key={feature.title} className="border-2 hover:border-primary/50 hover:shadow-lg transition-all">
                <CardHeader>
                  <div className="p-3 rounded-lg bg-primary/10 w-fit mb-2">
                    <Icon className="h-6 w-6 text-primary" />
                  </div>
                  <CardTitle className="text-xl">{feature.title}</CardTitle>
                  <CardDescription className="text-base">{feature.description}</CardDescription>
                </CardHeader>
              </Card>
            )
          })}
        </div>
      </section>

      {/* How It Works */}
      <section className="space-y-8">
        <div className="text-center space-y-2">
          <h2 className="text-3xl font-bold">How It Works</h2>
          <p className="text-muted-foreground">
            Simple workflow to get from monolith to microservices
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {steps.map((step, index) => {
            const Icon = step.icon
            return (
              <div key={step.number} className="relative h-full">
                {index < steps.length - 1 && (
                  <div className="hidden md:block absolute top-12 left-[60%] w-[80%] h-0.5 bg-gradient-to-r from-primary/50 to-transparent" />
                )}
                <Card className="relative border-2 h-full">
                  <CardHeader>
                    <div className="flex items-start gap-4">
                      <div className="flex-shrink-0">
                        <div className="text-5xl font-bold text-primary/20">{step.number}</div>
                      </div>
                      <div className="flex-1 space-y-2 pt-1">
                        <div className="p-2 rounded-lg bg-primary/10 w-fit">
                          <Icon className="h-5 w-5 text-primary" />
                        </div>
                        <CardTitle>{step.title}</CardTitle>
                        <CardDescription>{step.description}</CardDescription>
                      </div>
                    </div>
                  </CardHeader>
                </Card>
              </div>
            )
          })}
        </div>
      </section>

      {/* What You Get */}
      <section>
        <Card className="border-2 bg-gradient-to-br from-primary/5 to-primary/10">
          <CardHeader className="text-center pb-4">
            <CardTitle className="text-2xl">What You'll Get</CardTitle>
            <CardDescription>Comprehensive analysis and insights</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                'Dependency graphs with community detection algorithms',
                'Multiple decomposition strategies (Louvain, Leiden, Label Propagation)',
                'Domain-driven and actor-driven decomposition suggestions',
                'Load test benchmarks to compare architectures',
                'Performance metrics and comparative analysis',
                'Export results and recommendations',
              ].map((item) => (
                <div key={item} className="flex items-start gap-3">
                  <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0 mt-0.5" />
                  <span className="text-sm">{item}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </section>

      {/* CTA Section */}
      <section className="text-center space-y-6 py-12">
        <div className="space-y-3">
          <h2 className="text-3xl font-bold">Ready to Get Started?</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Begin your journey from monolith to microservices with data-driven insights
          </p>
        </div>
        <div className="flex items-center justify-center gap-4">
          <Link to="/projects/new">
            <Button size="lg" className="gap-2 shadow-lg shadow-primary/25">
              <FolderKanban className="h-5 w-5" />
              Create Your First Project
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
      </section>
    </div>
  )
}
