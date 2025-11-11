import * as React from 'react'
import { cn } from '@/lib/utils'
import { Upload } from 'lucide-react'

export interface FileInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  onFileChange?: (file: File | null) => void
}

const FileInput = React.forwardRef<HTMLInputElement, FileInputProps>(
  ({ className, onFileChange, disabled, ...props }, ref) => {
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0] || null
      onFileChange?.(file)
      props.onChange?.(e)
    }

    return (
      <div className="relative">
        <input
          type="file"
          className="sr-only"
          ref={ref}
          disabled={disabled}
          onChange={handleChange}
          {...props}
        />
        <label
          htmlFor={props.id}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg border-2 border-dashed cursor-pointer transition-all',
            'hover:border-primary hover:bg-primary/5',
            'focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2',
            disabled && 'opacity-50 cursor-not-allowed hover:border-input hover:bg-transparent',
            'active:scale-[0.98]',
            className
          )}
        >
          <Upload className={cn('h-4 w-4 text-muted-foreground transition-colors', !disabled && 'group-hover:text-primary')} />
          <span className="text-sm font-medium text-muted-foreground">
            Choose file or drag & drop
          </span>
        </label>
      </div>
    )
  }
)
FileInput.displayName = 'FileInput'

export { FileInput }
