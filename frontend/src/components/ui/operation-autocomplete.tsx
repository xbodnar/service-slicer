import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/input'
import { type ApiOperation } from '@/api/generated/openAPIDefinition.schemas'

interface OperationAutocompleteProps {
  value: string
  onChange: (value: string) => void
  onSelectOperation: (operation: ApiOperation) => void
  operations: ApiOperation[]
  placeholder?: string
  error?: string
}

export function OperationAutocomplete({ value, onChange, onSelectOperation, operations, placeholder, error }: OperationAutocompleteProps) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [filteredOps, setFilteredOps] = useState<ApiOperation[]>([])

  useEffect(() => {
    if (value && operations.length > 0) {
      const filtered = operations.filter(op =>
        op.operationId.toLowerCase().includes(value.toLowerCase()) ||
        op.path.toLowerCase().includes(value.toLowerCase())
      ).slice(0, 10)
      setFilteredOps(filtered)
    } else {
      setFilteredOps([])
      setShowSuggestions(false)
    }
  }, [value, operations])

  return (
    <div className="relative">
      <Input
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
          // Show suggestions when user is typing
          setShowSuggestions(true)
        }}
        onFocus={() => {
          // Only show suggestions if there's a partial match (not an exact match)
          const hasExactMatch = operations.some(op => op.operationId === value)
          if (!hasExactMatch && filteredOps.length > 0) {
            setShowSuggestions(true)
          }
        }}
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        placeholder={placeholder}
        className={error ? 'border-destructive' : ''}
      />
      {showSuggestions && filteredOps.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-auto">
          {filteredOps.map((op) => (
            <button
              key={op.id}
              type="button"
              className="w-full px-3 py-2 text-left hover:bg-gray-100 focus:bg-gray-100 focus:outline-none"
              onClick={() => {
                onSelectOperation(op)
                setShowSuggestions(false)
              }}
            >
              <div className="font-medium text-sm">{op.operationId}</div>
              <div className="text-xs text-muted-foreground">
                <span className="font-mono">{op.method}</span> {op.path}
              </div>
            </button>
          ))}
        </div>
      )}
      {error && <p className="text-sm text-destructive mt-1">{error}</p>}
    </div>
  )
}