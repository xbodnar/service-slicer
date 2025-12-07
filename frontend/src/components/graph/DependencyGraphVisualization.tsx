import { useMemo, useRef, useCallback, useEffect } from 'react'
import { BasicNvlWrapper } from '@neo4j-nvl/react'
import type { Node, Relationship, NVL } from '@neo4j-nvl/base'
import {ClassNodeDto} from "@/api/generated/openAPIDefinition.schemas.ts";

interface DependencyGraphVisualizationProps {
  nodes: ClassNodeDto[]
  clusterMapping?: Record<string, string> // className -> clusterId
  clusters?: Record<string, string[]> // clusterId -> classNames
  onNodeClick?: (node: ClassNodeDto) => void
}

const getPackageName = (fqn: string): string => {
  const parts = fqn.split('.')
  if (parts.length > 2) {
    return parts.slice(0, parts.length - 1).join('.')
  }
  return parts[0] || 'default'
}

const getColorForPackage = (packageName: string): string => {
  const colors = [
    '#3b82f6', // blue
    '#10b981', // green
    '#f59e0b', // amber
    '#ef4444', // red
    '#8b5cf6', // purple
    '#ec4899', // pink
    '#06b6d4', // cyan
    '#84cc16', // lime
  ]

  const hash = packageName.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + ((acc << 5) - acc)
  }, 0)

  return colors[Math.abs(hash) % colors.length]
}

const getColorForCluster = (clusterId: string): string => {
  const colors = [
    '#3b82f6', // blue
    '#10b981', // green
    '#f59e0b', // amber
    '#ef4444', // red
    '#8b5cf6', // purple
    '#ec4899', // pink
    '#06b6d4', // cyan
    '#84cc16', // lime
    '#f97316', // orange
    '#14b8a6', // teal
    '#a855f7', // violet
    '#f43f5e', // rose
    '#6366f1', // indigo
    '#22c55e', // green-500
    '#eab308', // yellow
    '#64748b', // slate
  ]

  const hash = clusterId.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + ((acc << 5) - acc)
  }, 0)

  return colors[Math.abs(hash) % colors.length]
}

export function DependencyGraphVisualization({
  nodes: classNodes,
  clusterMapping,
  clusters,
  onNodeClick,
}: DependencyGraphVisualizationProps) {
  const nvlRef = useRef<NVL | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const setNvlRef = useCallback((instance: NVL | null) => {
    if (instance && !nvlRef.current) {
      nvlRef.current = instance
    }
  }, [])

  // Prevent page scrolling when using mouse wheel on graph
  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const handleWheel = (e: WheelEvent) => {
      e.preventDefault()
    }

    container.addEventListener('wheel', handleWheel, { passive: false })
    return () => container.removeEventListener('wheel', handleWheel)
  }, [])

  // Transform data to NVL format
  const { nvlNodes, nvlRelationships, classNodeMap } = useMemo(() => {
    // Create a map for quick lookup
    const classNodeMap = new Map<string, ClassNodeDto>()
    classNodes.forEach((node) => {
      classNodeMap.set(node.fullyQualifiedClassName, node)
    })

    // Transform to NVL nodes
    const nvlNodes: Node[] = classNodes.map((classNode) => {
      let color: string

      // Use cluster coloring if cluster mapping is provided
      if (clusterMapping) {
        const clusterId = clusterMapping[classNode.fullyQualifiedClassName]
        color = clusterId ? getColorForCluster(clusterId) : '#64748b' // gray for unassigned
      } else {
        // Fall back to package-based coloring
        const packageName = getPackageName(classNode.fullyQualifiedClassName)
        color = getColorForPackage(packageName)
      }

      return {
        id: classNode.fullyQualifiedClassName,
        caption: classNode.simpleClassName,
        color,
        size: 25,
      }
    })

    // Transform to NVL relationships
    const nvlRelationships: Relationship[] = []
    const nodeIds = new Set(classNodes.map((n) => n.fullyQualifiedClassName))

    classNodes.forEach((classNode) => {
      classNode.dependencies.forEach((dependency) => {
        // Only create relationships for dependencies that exist in our node set
        if (nodeIds.has(dependency)) {
          nvlRelationships.push({
            id: `${classNode.fullyQualifiedClassName}->${dependency}`,
            from: classNode.fullyQualifiedClassName,
            to: dependency,
            caption: '',
          })
        }
      })
    })

    return { nvlNodes, nvlRelationships, classNodeMap }
  }, [classNodes, clusterMapping])

  // Setup interaction handlers
  useEffect(() => {
    if (!nvlRef.current) return

    const nvl = nvlRef.current
    let panHandler: any
    let zoomHandler: any
    let clickHandler: any

    import('@neo4j-nvl/interaction-handlers').then(({ PanInteraction, ZoomInteraction, ClickInteraction }) => {
      panHandler = new PanInteraction(nvl)
      zoomHandler = new ZoomInteraction(nvl)
      clickHandler = new ClickInteraction(nvl)

      clickHandler.updateCallback('onNodeClick', (node: Node) => {
        if (onNodeClick) {
          const classNode = classNodeMap.get(node.id)
          if (classNode) {
            onNodeClick(classNode)
          }
        }
      })
    })

    return () => {
      panHandler?.destroy()
      zoomHandler?.destroy()
      clickHandler?.destroy()
    }
  }, [classNodeMap, onNodeClick])

  const handleZoomIn = useCallback(() => {
    if (nvlRef.current) {
      const currentZoom = nvlRef.current.getScale()
      nvlRef.current.setZoom(currentZoom * 1.3)
    }
  }, [])

  const handleZoomOut = useCallback(() => {
    if (nvlRef.current) {
      const currentZoom = nvlRef.current.getScale()
      nvlRef.current.setZoom(currentZoom / 1.3)
    }
  }, [])

  const handleResetView = useCallback(() => {
    if (nvlRef.current) {
      const allNodeIds = classNodes.map(n => n.fullyQualifiedClassName)
      nvlRef.current.fit(allNodeIds)
    }
  }, [classNodes])

  if (classNodes.length === 0) {
    return (
      <div className="flex items-center justify-center h-[800px] bg-slate-900 rounded-lg border-2 border-dashed border-slate-700">
        <p className="text-slate-400">No graph data available</p>
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      className="h-[800px] bg-slate-900 rounded-lg border border-slate-700 relative"
    >
      <BasicNvlWrapper
        nodes={nvlNodes}
        rels={nvlRelationships}
        nvlOptions={{
          initialZoom: 0.5,
          minZoom: 0.05,
          maxZoom: 10,
          allowDynamicMinZoom: false,
          disableWebGL: true,
          instanceId: 'dependency-graph',
          relationshipThreshold: 0.55,
        }}
        layout="forceDirected"
        nvlCallbacks={{
          onLayoutDone: () => {
            setTimeout(() => {
              handleResetView()
            }, 500)
          },
        }}
        style={{
          width: '100%',
          height: '100%',
          pointerEvents: 'auto',
          touchAction: 'none',
        }}
        className="nvl-wrapper"
        ref={setNvlRef}
      />

      {/* Debug info */}
      <div className="absolute top-2 left-2 bg-slate-800 text-white px-2 py-1 rounded text-xs z-50 pointer-events-none">
        Nodes: {nvlNodes.length} | Edges: {nvlRelationships.length}
      </div>

      {/* Legend */}
      {clusters && Object.keys(clusters).length > 0 && (
        <div className="absolute top-2 right-2 bg-slate-800 text-white px-3 py-2 rounded text-xs z-50 max-h-[300px] overflow-y-auto">
          <div className="font-semibold mb-2">Clusters</div>
          <div className="space-y-1">
            {Object.entries(clusters)
              .sort(([a], [b]) => a.localeCompare(b))
              .map(([clusterId, classNames]) => (
                <div key={clusterId} className="flex items-center gap-2">
                  <div
                    className="w-3 h-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: getColorForCluster(clusterId) }}
                  />
                  <span className="text-xs">
                    {clusterId} ({classNames.length})
                  </span>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* Zoom controls */}
      <div className="absolute bottom-4 right-4 flex flex-col gap-2 z-50">
        <button
          onClick={handleZoomIn}
          className="bg-slate-800 hover:bg-slate-700 text-white p-2 rounded shadow-lg border border-slate-600 transition-colors"
          title="Zoom In"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
        </button>
        <button
          onClick={handleZoomOut}
          className="bg-slate-800 hover:bg-slate-700 text-white p-2 rounded shadow-lg border border-slate-600 transition-colors"
          title="Zoom Out"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
          </svg>
        </button>
        <button
          onClick={handleResetView}
          className="bg-slate-800 hover:bg-slate-700 text-white p-2 rounded shadow-lg border border-slate-600 transition-colors"
          title="Fit to Screen"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
          </svg>
        </button>
      </div>

      {/* Instructions */}
      <div className="absolute bottom-4 left-4 bg-slate-800 text-white px-3 py-2 rounded text-xs z-50 pointer-events-none">
        <div className="font-semibold mb-1">Controls:</div>
        <div>• Mouse wheel to zoom</div>
        <div>• Click + drag to pan</div>
        <div>• Click node for details</div>
      </div>
    </div>
  )
}
