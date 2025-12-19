import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import type { Config } from '@sveltejs/kit';
// For Svelte 5, we don't need to import Warning type explicitly
// Just use any type for warning parameter

/** @type {Config} */
const config = {
	// Consult https://svelte.dev/docs/kit/integrations
	// for more information about preprocessors
	preprocess: vitePreprocess(),

	compilerOptions: {
		// These warnings are from generated SvelteKit code, not our own
		// They're expected and don't affect functionality
		dev: false
	},

	// Filter out warnings from generated SvelteKit files
	onwarn: (warning: any, handler: (warning: any) => void) => {
		// Ignore warnings from generated files
		if (warning.filename?.includes('.svelte-kit/generated/')) {
			return;
		}
		
		// Ignore state referenced locally warnings from Svelte 5
		if (warning.code === 'state_referenced_locally') {
			return;
		}
		
		// Otherwise, pass the warning to the default handler
		handler(warning);
	},

	kit: {
		// Configure for serving from /app path
		paths: {
			base: '/app'
		},
		// Use static adapter for serving with FastAPI in SPA mode
		adapter: adapter({
			// Use SPA mode with fallback for client-side routing
			fallback: 'index.html',
			pages: 'build',
			assets: 'build',
			precompress: false,
			strict: false
		})
	}
};

export default config;
