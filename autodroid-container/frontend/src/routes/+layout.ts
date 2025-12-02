// Disable SSR for all routes to avoid hydration mismatches in SPA mode
export const ssr = false;

// Enable client-side rendering
export const csr = true;

// Disable prerendering since we're using a dynamic app
export const prerender = false;

// Authentication state management
interface AuthState {
	isAuthenticated: boolean;
	user: {
		id: string;
		email: string;
		full_name: string;
	} | null;
}

// Standard SvelteKit load function
export function load() {
	// Load authentication state from localStorage
	const token = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
	const userData = typeof window !== 'undefined' ? localStorage.getItem('user_data') : null;
	
	let authState: AuthState = {
		isAuthenticated: false,
		user: null
	};
	
	if (token && userData) {
		try {
			const user = JSON.parse(userData);
			authState = {
				isAuthenticated: true,
				user: user
			};
		} catch {
			// Invalid user data, clear storage
			if (typeof window !== 'undefined') {
				localStorage.removeItem('auth_token');
				localStorage.removeItem('user_data');
			}
		}
	}
	
	return {
		authState: authState
	};
}

// Check if a route requires authentication (internal function)
function _requiresAuth(pathname: string): boolean {
	const publicRoutes = ['/auth', '/auth/login'];
	return !publicRoutes.some(route => pathname.startsWith(route));
}

export { _requiresAuth };
