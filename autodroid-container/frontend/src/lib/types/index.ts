// Server related types
export interface ServerInfo {
    name: string;
    version: string;
    hostname: string;
    ip_address: string;
    platform: string;
    python_version: string;
    services: {
        device_manager: string;
        workflow_engine: string;
        scheduler: string;
    };
    capabilities: {
        device_registration: boolean;
        workflow_execution: boolean;
        test_scheduling: boolean;
        event_triggering: boolean;
    };
    api_endpoints: {
        health: string;
        devices: string;
        device_register: string;
        workflows: string;
        test_plans: string;
    };
}

export interface WiFiNetwork {
    name: string;
    signal_strength: number;
    security: string;
    ip_address?: string;
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
    udid: string;
    model: string;
    manufacturer: string;
    version: string;
    sdk_version: number;
    ip_address: string;
    status: string;
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

// Workflow related types
export interface WorkflowStep {
    id: string;
    type: string;
    action: string;
    selector: string;
    value?: any;
    timeout?: number;
    [key: string]: any;
}

export interface WorkflowConfig {
    id: string;
    name: string;
    description: string;
    metadata: Record<string, string>;
    device_selection: Record<string, any>;
    steps: WorkflowStep[];
    schedule?: Record<string, any>;
}

export interface WorkflowCreate {
    id: string;
    name: string;
    description: string;
    metadata: Record<string, string>;
    device_selection: Record<string, any>;
    steps: WorkflowStep[];
    schedule?: Record<string, any>;
}

export interface WorkflowInfo {
    id: string;
    name: string;
    description: string;
    metadata: Record<string, string>;
    device_selection: Record<string, any>;
    steps: WorkflowStep[];
    schedule?: Record<string, any>;
}

// Workflow plan related types
export interface WorkflowPlanCreate {
    workflow_id: string;
    device_udid: string;
    enabled?: boolean;
    schedule: Record<string, any>;
    priority?: number;
}

export interface DeviceWorkflowPlan {
    id: string;
    workflow_id: string;
    device_udid: string;
    enabled: boolean;
    schedule: Record<string, any>;
    priority: number;
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
        workflow_engine: string;
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
export interface UserProfile {
    id: string;
    username: string;
    email: string;
    name: string;
    role: string;
    last_login: string;
    created_at: string;
    [ key: string ]: any;
}

export interface UserPreferences {
    theme: string;
    language: string;
    notifications: boolean;
    email_alerts: boolean;
    auto_refresh: boolean;
    refresh_interval: number;
    [ key: string ]: any;
}

// Order related types
export interface Order {
    id: string;
    name: string;
    description: string;
    workflow_name: string;
    device_udid: string;
    status: string;
    created_at: string;
    updated_at: string;
    priority: number;
    [ key: string ]: any;
}

// Report related types
export interface TestReportStep {
    id: string;
    action: string;
    status: string;
    duration: number;
    error?: string;
    [ key: string ]: any;
}

export interface TestReport {
    id: string;
    name: string;
    description: string;
    workflow_name: string;
    device_udid: string;
    status: string;
    start_time: string;
    end_time: string;
    duration: number;
    steps: TestReportStep[];
    [ key: string ]: any;
}

// API response types
export interface ApiResponse<T = any> {
    message?: string;
    data?: T;
    [key: string]: any;
}

export interface DevicesResponse {
    devices: DeviceInfo[];
}

export interface WorkflowsResponse {
    workflows: WorkflowInfo[];
}

export interface WorkflowPlansResponse {
    plans: DeviceWorkflowPlan[];
}
