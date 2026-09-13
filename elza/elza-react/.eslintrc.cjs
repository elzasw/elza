// Zpráva u zákazu legacy i18n - ať je v chybě rovnou vidět, co místo toho.
const LEGACY_I18N = [
  'Legacy i18n() je jen česky (window.messages z messages_cs.js).',
  'Nový text patří do react-intl: defineMessages + <FormattedMessage> / useIntl.',
  'Viz .claude/rules/i18n.md.',
].join(' ');

module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh'],
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    // Většina volajících nebere helper přímo, ale přes barrel
    // (`import { i18n } from 'components/shared'`), takže bez `importNames`
    // by pravidlo nechytilo skoro nic.
    'no-restricted-imports': ['error', {
      paths: [
        { name: 'components/i18n', message: LEGACY_I18N },
        { name: 'components/shared', importNames: ['i18n'], message: LEGACY_I18N },
        { name: 'components', importNames: ['i18n'], message: LEGACY_I18N },
      ],
      patterns: [
        { group: ['**/components/i18n'], message: LEGACY_I18N },
      ],
    }],
  },
  overrides: [
    {
      // Existující legacy soubory pravidlo neřeší - dluh hlídá shrink-only
      // ratchet (`npm run i18n:legacy-check`), který navíc pokrývá i .jsx,
      // kam `npm run lint` vůbec nechodí. ESLint tu má bránit jen tomu, aby
      // legacy helper přibyl do nově psaného kódu.
      files: ['src/**/*.test.{ts,tsx}', 'src/test/**'],
      rules: { 'no-restricted-imports': 'off' },
    },
  ],
}
