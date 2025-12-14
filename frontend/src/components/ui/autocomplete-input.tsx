import { useState, useEffect } from 'react'
import { Input } from '@/components/ui/input'

interface AutocompleteInputProps {
  value: string
  onChange: (value: string) => void
  suggestions: string[]
  placeholder?: string
  className?: string
}

export function AutocompleteInput({ value, onChange, suggestions, placeholder, className }: AutocompleteInputProps) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [filteredSuggestions, setFilteredSuggestions] = useState<string[]>([])

  useEffect(() => {
    if (value && suggestions.length > 0) {
      const filtered = suggestions.filter(s =>
        s.toLowerCase().includes(value.toLowerCase())
      ).slice(0, 8)
      setFilteredSuggestions(filtered)
    } else {
      setFilteredSuggestions([])
      setShowSuggestions(false)
    }
  }, [value, suggestions])

  return (
    <div className="relative flex-1">
      <Input
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
          // Show suggestions when user is typing
          setShowSuggestions(true)
        }}
        onFocus={() => {
          // Only show suggestions if there's a partial match (not an exact match)
          const hasExactMatch = suggestions.some(s => s.toLowerCase() === value.toLowerCase())
          if (!hasExactMatch && filteredSuggestions.length > 0) {
            setShowSuggestions(true)
          }
        }}
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        placeholder={placeholder}
        className={className}
      />
      {showSuggestions && filteredSuggestions.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-40 overflow-auto">
          {filteredSuggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              className="w-full px-2 py-1.5 text-left text-xs hover:bg-gray-100 focus:bg-gray-100 focus:outline-none"
              onClick={() => {
                onChange(suggestion)
                setShowSuggestions(false)
              }}
            >
              {suggestion}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
