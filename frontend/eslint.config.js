import js from '@eslint/js'
import ts from 'typescript-eslint'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
export default ts.config(
  { ignores: ['dist/**', 'node_modules/**'] },
  js.configs.recommended,
  ...ts.configs.recommended,
  ...vue.configs['flat/recommended'],
  { files: ['**/*.{ts,vue}'], languageOptions: { parserOptions: { parser: ts.parser }, globals: globals.browser }, rules: {
    'vue/multi-word-component-names': 'off', 'vue/html-self-closing': 'off', 'vue/max-attributes-per-line': 'off', 'vue/singleline-html-element-content-newline': 'off',
    'vue/html-indent': 'off',
    '@typescript-eslint/no-explicit-any': 'error', '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
  } },
  { files: ['src/components/MarkdownView.vue'], rules: { 'vue/no-v-html': 'off' } },
)
