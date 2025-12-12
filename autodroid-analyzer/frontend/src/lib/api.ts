import type {
	ApiResponse,
	ApkInfo,
	AutomationScript,
	PageElement,
	ScreenshotInfo,
	UserOperation
} from './types';

class ApiClient {
	private baseUrl = 'http://localhost:8001/api';

	async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
		try {
			const response = await fetch(`${this.baseUrl}${endpoint}`, {
				headers: {
					'Content-Type': 'application/json',
					...options.headers
				},
				...options
			});

			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`);
			}

			const data = await response.json();
			return { success: true, data };
		} catch (error) {
			return {
				success: false,
				error: error instanceof Error ? error.message : 'Unknown error occurred'
			};
		}
	}

	// APK相关API
	async getApks(): Promise<ApiResponse<ApkInfo[]>> {
		return this.request<ApkInfo[]>('/apks');
	}

	async getApk(apkId: string): Promise<ApiResponse<ApkInfo>> {
		return this.request<ApkInfo>(`/apks/${apkId}`);
	}

	// 操作记录相关API（按时间倒序排列）
	async getOperationsByApk(apkId: string, limit: number = 100): Promise<ApiResponse<UserOperation[]>> {
		return this.request<UserOperation[]>(`/apks/${apkId}/operations?limit=${limit}&sort=desc`);
	}

	async getOperationsByTimeRange(apkId: string, startTime: string, endTime: string): Promise<ApiResponse<UserOperation[]>> {
		return this.request<UserOperation[]>(`/apks/${apkId}/operations?start_time=${startTime}&end_time=${endTime}&sort=desc`);
	}

	async deleteOperation(operationId: string): Promise<ApiResponse<void>> {
		return this.request<void>(`/operations/${operationId}`, { method: 'DELETE' });
	}

	// 截屏相关API
	async getScreenshotsByApk(apkId: string): Promise<ApiResponse<ScreenshotInfo[]>> {
		return this.request<ScreenshotInfo[]>(`/apks/${apkId}/screenshots`);
	}

	async getScreenshot(screenshotId: string): Promise<ApiResponse<ScreenshotInfo>> {
		return this.request<ScreenshotInfo>(`/screenshots/${screenshotId}`);
	}

	async associateOperationWithScreenshot(operationId: string, screenshotId: string): Promise<ApiResponse<void>> {
		return this.request<void>(`/operations/${operationId}/associate`, {
			method: 'POST',
			body: JSON.stringify({ screenshot_id: screenshotId })
		});
	}



	// 页面元素相关API
	async getElements(screenshotId: string): Promise<ApiResponse<PageElement[]>> {
		return this.request<PageElement[]>(`/screenshots/${screenshotId}/elements`);
	}

	async updateElement(elementId: string, updates: Partial<PageElement>): Promise<ApiResponse<PageElement>> {
		return this.request<PageElement>(`/elements/${elementId}`, {
			method: 'PUT',
			body: JSON.stringify(updates)
		});
	}

	async deleteElement(elementId: string): Promise<ApiResponse<void>> {
		return this.request<void>(`/elements/${elementId}`, { method: 'DELETE' });
	}

	// 自动化脚本相关API
	async generateScript(apkId: string, config: any): Promise<ApiResponse<AutomationScript>> {
		return this.request<AutomationScript>(`/apks/${apkId}/generate-script`, {
			method: 'POST',
			body: JSON.stringify(config)
		});
	}

	async getScripts(apkId: string): Promise<ApiResponse<AutomationScript[]>> {
		return this.request<AutomationScript[]>(`/apks/${apkId}/scripts`);
	}
}

export const api = new ApiClient();