import { defineConfig } from 'orval'

export default defineConfig({
  serviceSlicerApi: {
    input: {
      target: 'http://localhost:8080/v3/api-docs',
      // Alternatively, you can save the OpenAPI spec and use a local file:
      // target: './openapi.json',
    },
    output: {
      mode: 'tags-split',
      target: './src/api/generated',
      client: 'react-query',
      httpClient: 'axios',
      mock: false,
      clean: true,
      prettier: false, // Disable prettier to avoid issues
      override: {
        mutator: {
          path: './src/api/client.ts',
          name: 'apiClient',
        },
        query: {
          useQuery: true,
          useSuspenseQuery: false,
          signal: true,
        },
      },
    },
    // Remove the problematic hook - you can run lint manually if needed
  },
})
