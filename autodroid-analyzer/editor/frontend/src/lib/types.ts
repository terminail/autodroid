// APK信息类型定义
export interface ApkInfo {
	id: string;
	package_name: string;
	app_name: string;
	version: string;
	version_name: string;
	version_code?: number;
	install_time: string;
	last_analyzed?: string;
	total_screenshots: number;
	total_operations: number;
	last_operation_time?: string;
	is_packed?: boolean;
	packer_type?: string;
	packer_confidence?: number;
	packer_indicators?: string;
	packer_analysis_time?: string;
}

// 用户操作类型定义
export interface UserOperation {
	id: string;
	timestamp: string;
	action_type: 'click' | 'input' | 'swipe' | 'wait' | 'back' | 'home';
	target_element?: string;
	target_text?: string;
	coordinates?: { x: number; y: number };
	duration?: number;
	apk_id: string;
	src_screenshot_id?: string;
	dest_screenshot_id?: string;
}

// 截屏信息类型定义
export interface ScreenshotInfo {
	id: string;
	timestamp: string;
	file_path: string;
	apk_id: string;
	elements?: PageElement[];
}

// 页面元素类型定义
export interface PageElement {
	id: string;
	type: 'button' | 'text' | 'input' | 'image' | 'container' | 'unknown';
	text?: string;
	bounds: { left: number; top: number; right: number; bottom: number };
	importance: number; // 1-5 重要性评分
	custom_tags?: string[];
	screenshot_id: string;
}

// 自动化脚本类型定义
export interface AutomationScript {
	id: string;
	name: string;
	type: 'adb_shell' | 'python_adb' | 'appium';
	steps: ScriptStep[];
	created_at: string;
	apk_id: string;
}

// 脚本步骤类型定义
export interface ScriptStep {
	type: 'click' | 'input' | 'swipe' | 'wait';
	target?: string;
	text?: string;
	coordinates?: { x: number; y: number };
	duration?: number;
}

// API响应类型定义
export interface ApiResponse<T> {
	success: boolean;
	data?: T;
	error?: string;
}

