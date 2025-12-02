import type { ServerConfig, DashboardConfig, LoggingConfig, UIConfig } from '../types';

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
        url: 'http://localhost:8003',
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

// Load configuration from config.yaml file
export async function loadConfig(): Promise<AppConfig> {
    try {
        // In a real application, we would fetch this from the server or use a static import
        // For now, we'll just return the default config
        // const response = await fetch('/config.yaml');
        // if (!response.ok) {
        //     throw new Error('Failed to load config');
        // }
        // const configText = await response.text();
        // return yaml.parse(configText) as AppConfig;
        
        return defaultConfig;
    } catch (error) {
        console.warn('Failed to load config, using default:', error);
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
