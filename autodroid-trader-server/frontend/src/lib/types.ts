// Server related types
export interface ServerInfo {
    name: string;
    version: string;
    hostname: string;
    ipAddress: string;
    platform: string;
    python_version: string;
    services: {
        device_manager: string;
    };
    capabilities: {
        device_registration: boolean;
        test_scheduling: boolean;
        event_triggering: boolean;
    };
    api_endpoints: {
        health: string;
        devices: string;
        device_register: string;

        test_plans: string;
    };
}

// Server data type for dashboard
export interface ServerData {
    name: string;
    hostname: string;
    ipAddress: string;
    platform: string;
    api_base_url?: string;
}

export interface WiFiNetwork {
    name: string;
    signal_strength: number;
    security: string;
    ipAddress?: string;
}

export interface ServerWiFiInfo {
    server_ip: string;
    server_subnet: string;
    wifis: WiFiNetwork[];
    suggested_wifi: WiFiNetwork | null;
    platform: string;
}

// Device related types
export interface DeviceInfo {
    id: string;
    name: string;
    model: string;
    os_version: string;
    status: 'online' | 'offline' | 'busy';
    ipAddress: string;
    udid?: string;
    manufacturer?: string;
    version?: string;
    sdk_version?: number;
    apks?: APKInfo[];
    [key: string]: any;
}

export interface APKInfo {
    package_name: string;
    version_name: string;
    version_code: number;
    install_time: string;
    [key: string]: any;
}



// Event related types
export interface EventTrigger {
    event_type: string;
    event_data: Record<string, any>;
}

// Health check type
export interface HealthCheck {
    status: string;
    timestamp: number;
    services: {
        device_manager: string;
        scheduler: string;
    };
}

// Configuration types
export interface ServerConfig {
    url: string;
    api_base: string;
    use_https: boolean;
    timeout: number;
}

export interface DashboardConfig {
    refresh_interval: number;
    show_connection_suggestions: boolean;
    show_wifi_details: boolean;
}

export interface LoggingConfig {
    level: 'debug' | 'info' | 'warn' | 'error';
    log_api_requests: boolean;
}

export interface UIConfig {
    default_theme: 'light' | 'dark' | 'system';
    default_language: string;
    show_tooltips: boolean;
    show_help_text: boolean;
}

export interface AppConfig {
    server: ServerConfig;
    dashboard: DashboardConfig;
    logging: LoggingConfig;
    ui: UIConfig;
}

// User related types
export interface UserInfo {
    id: string;
    email: string;
    name: string;
    role: 'admin' | 'user';
    last_login: string;
    created_at: string;
}

export interface UserProfile {
    id: string;
    username: string;
    email: string;
    name: string;
    role: string;
    last_login: string;
    created_at: string;
    [key: string]: any;
}

export interface UserPreferences {
    theme: string;
    language: string;
    notifications: boolean;
    email_alerts: boolean;
    auto_refresh: boolean;
    refresh_interval: number;
    [key: string]: any;
}

// Order related types
export interface Order {
    id: string;
    name: string;
    description: string;
    device_udid: string;
    status: string;
    created_at: string;
    updated_at: string;
    priority: number;
    [key: string]: any;
}

// Report related types
export interface TestReportStep {
    id: string;
    action: string;
    status: string;
    duration: number;
    error?: string;
    [key: string]: any;
}

export interface TestReport {
    id: string;
    name: string;
    description: string;
    device_udid: string;
    status: string;
    start_time: string;
    end_time: string;
    duration: number;
    steps: TestReportStep[];
    [key: string]: any;
}

// API response types
export interface ApiResponse<T = any> {
    success?: boolean;
    message?: string;
    data?: T;
    error?: string;
}

export interface DevicesResponse {
    devices: DeviceInfo[];
}

