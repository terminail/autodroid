// API and Application Configuration
import type { ServerConfig, DashboardConfig, LoggingConfig, UIConfig } from './types';

// Configuration types
export interface AppConfig {
    server: ServerConfig;
    dashboard: DashboardConfig;
    logging: LoggingConfig;
    ui: UIConfig;
}

// Default configuration
export const defaultConfig: AppConfig = {
    server: {
        url: 'http://127.0.0.1:8004',
        api_base: '/api',
        use_https: false,
        timeout: 10000
    },
    dashboard: {
        refresh_interval: 30,
        show_connection_suggestions: true,
        show_wifi_details: true
    },
    logging: {
        level: 'info',
        log_api_requests: false
    },
    ui: {
        default_theme: 'light',
        default_language: 'en',
        show_tooltips: true,
        show_help_text: true
    }
};

// API Configuration - simplified for direct use
export const API_CONFIG = {
    // Use relative URL to avoid hardcoding the server URL
    BASE_URL: '',
    API_PREFIX: '/api',
    
    // Full API URL
    get API_URL() {
        return `${this.BASE_URL}${this.API_PREFIX}`;
    },
    
    // Authentication endpoints
    get AUTH_URL() {
        return `${this.API_URL}/auth`;
    },
    
    // Specific auth endpoints
    get LOGIN_URL() {
        return `${this.AUTH_URL}/login`;
    },
    
    get REGISTER_URL() {
        return `${this.AUTH_URL}/register`;
    },
    
    get ME_URL() {
        return `${this.AUTH_URL}/me`;
    },
    
    get LOGOUT_URL() {
        return `${this.AUTH_URL}/logout`;
    }
};

// Frontend configuration
export const FRONTEND_CONFIG = {
    // Base path for the frontend app
    BASE_PATH: '/app',
    
    // Authentication paths
    get LOGIN_PATH() {
        return `${this.BASE_PATH}/auth/login`;
    },
    
    get REGISTER_PATH() {
        return `${this.BASE_PATH}/auth`;
    }
};

// Load configuration from unified config.yaml file
export async function loadConfig(): Promise<AppConfig> {
    try {
        // Fetch the unified configuration from the backend server
        const response = await fetch('/api/config');
        if (!response.ok) {
            throw new Error(`Failed to load config: ${response.status} ${response.statusText}`);
        }
        const unifiedConfig = await response.json();
        
        // Extract frontend-specific configuration from unified config
        const frontendConfig = unifiedConfig.frontend || {};
        const serverConfig = unifiedConfig.server || {};
        const networkConfig = unifiedConfig.network || {};
        const mdnsConfig = networkConfig.mdns || {};
        
        // Get the current server URL from the browser
        const currentUrl = window.location.origin;
        
        // Merge with defaults
        return {
            server: {
                url: currentUrl,
                api_base: serverConfig.backend?.api_base || defaultConfig.server.api_base,
                use_https: serverConfig.backend?.use_https || defaultConfig.server.use_https,
                timeout: serverConfig.backend?.timeout || defaultConfig.server.timeout
            },
            dashboard: {
                refresh_interval: frontendConfig.refresh_interval || defaultConfig.dashboard.refresh_interval,
                show_connection_suggestions: frontendConfig.show_connection_suggestions !== undefined ? frontendConfig.show_connection_suggestions : defaultConfig.dashboard.show_connection_suggestions,
                show_wifi_details: frontendConfig.show_wifi_details !== undefined ? frontendConfig.show_wifi_details : defaultConfig.dashboard.show_wifi_details
            },
            logging: {
                level: frontendConfig.log_level || defaultConfig.logging.level,
                log_api_requests: frontendConfig.log_api_requests !== undefined ? frontendConfig.log_api_requests : defaultConfig.logging.log_api_requests
            },
            ui: {
                default_theme: frontendConfig.default_theme || defaultConfig.ui.default_theme,
                default_language: frontendConfig.default_language || defaultConfig.ui.default_language,
                show_tooltips: frontendConfig.show_tooltips !== undefined ? frontendConfig.show_tooltips : defaultConfig.ui.show_tooltips,
                show_help_text: frontendConfig.show_help_text !== undefined ? frontendConfig.show_help_text : defaultConfig.ui.show_help_text
            }
        };
    } catch (error) {
        console.warn('Failed to load unified config, using default:', error);
        return defaultConfig;
    }
}

// Get the full API URL for a given endpoint
export function getApiUrl(endpoint: string, config: AppConfig = defaultConfig): string {
    const { url, api_base } = config.server;
    // Ensure the API base starts with a slash
    const normalizedApiBase = api_base.startsWith('/') ? api_base : `/${api_base}`;
    // Ensure the endpoint doesn't start with a slash
    const normalizedEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
    // Combine all parts
    return `${url}${normalizedApiBase}/${normalizedEndpoint}`;
}