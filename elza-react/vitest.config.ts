import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
    resolve: {
        alias: {
            'src': path.resolve(__dirname, './src/'),
            'stores': path.resolve(__dirname, './src/stores/'),
            'components': path.resolve(__dirname, './src/components/'),
            'actions': path.resolve(__dirname, './src/actions/'),
            'pages': path.resolve(__dirname, './src/pages/'),
            'api': path.resolve(__dirname, './src/api/'),
            'utils': path.resolve(__dirname, './src/utils/'),
            'typings': path.resolve(__dirname, './src/typings/'),
            'contexts': path.resolve(__dirname, './src/contexts/'),
            'shared': path.resolve(__dirname, './src/shared/'),
            'test': path.resolve(__dirname, './src/test/'),
        },
    },
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
        css: false,
        include: ['src/**/*.{test,spec}.{ts,tsx}'],
        exclude: ['node_modules', 'dist', 'target'],
        clearMocks: true,
        restoreMocks: true,
        coverage: {
            provider: 'v8',
            reporter: ['text', 'html'],
            include: ['src/**/*.{ts,tsx,js,jsx}'],
            exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/api/generated/**'],
        },
    },
});
