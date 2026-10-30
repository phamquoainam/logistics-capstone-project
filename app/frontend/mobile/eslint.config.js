const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  { ignores: ['android/**', 'ios/**', '.expo/**', 'dist/**', 'node_modules/**'] },
  expoConfig,
]);
