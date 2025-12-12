import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		hmr: false,
		proxy: {
			'/api': {
				target: 'http://localhost:8004',
				changeOrigin: true,
				secure: false
			}
		}
	}
});
