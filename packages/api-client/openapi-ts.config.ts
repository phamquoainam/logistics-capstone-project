import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../../contracts/openapi/demo-001.yaml',
  output: {
    path: 'src/generated',
    clean: false,
    module: { extension: '.js' },
    tsConfigPath: './tsconfig.json',
    postProcess: [],
    source: false,
  },
  interactive: false,
  logs: { level: 'error', file: false },
  parser: {
    patch: {
      // 0.99.0 otherwise emits `unknown` for typed strings with pattern-only
      // allOf branches. The parent already asserts string, so this redundant
      // type preserves the accepted schema's validation set and constraints.
      schemas: (_name, schema) => {
        for (const property of Object.values(schema.properties ?? {})) {
          if (typeof property !== 'object' || property === null || property.type !== 'string') continue;
          for (const branch of property.allOf ?? []) {
            if (typeof branch === 'object' && branch !== null &&
              Object.keys(branch).length === 1 && typeof branch.pattern === 'string') branch.type = 'string';
          }
        }
      },
    },
  },
  plugins: [
    '@hey-api/typescript',
    { name: '@hey-api/sdk', auth: true, operations: 'flat' },
    { name: '@hey-api/client-fetch', bundle: true, baseUrl: true },
  ],
});
