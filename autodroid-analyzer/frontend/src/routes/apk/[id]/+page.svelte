<script lang="ts">
	import { goto } from "$app/navigation";
	import { api } from "$lib/api";
	import Header from "$lib/components/Header.svelte";
	import type { ApkInfo, UserOperation } from "$lib/types";
	import { onMount } from "svelte";

	// 路由参数
	let { data } = $props();

	// APK相关状态
	let apk: ApkInfo | null = null;
	let operations: UserOperation[] = [];
	let loading = true;
	let error: string | null = null;

	onMount(async () => {
		try {
			const response = await api.getApk(data.params.id);
			if (response.success) {
				apk = response.data;
				operations = response.data?.operations || [];
			} else {
				error = response.error || "获取APK详情失败";
			}
		} catch (err) {
			error = "网络错误，请检查服务器连接";
		} finally {
			loading = false;
		}
	});

	function formatDate(dateString: string): string {
		return new Date(dateString).toLocaleString("zh-CN");
	}

	function formatTimeAgo(dateString: string): string {
		const now = new Date();
		const date = new Date(dateString);
		const diffMs = now.getTime() - date.getTime();
		const diffMins = Math.floor(diffMs / (1000 * 60));
		const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
		const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

		if (diffMins < 1) return "刚刚";
		if (diffMins < 60) return `${diffMins}分钟前`;
		if (diffHours < 24) return `${diffHours}小时前`;
		if (diffDays < 7) return `${diffDays}天前`;
		return formatDate(dateString);
	}

	// 处理Tab切换
	function handleTabChange(tab: "devices" | "apks") {
		if (tab === "devices") {
			goto("/device");
		} else {
			goto("/apk");
		}
	}
</script>

<svelte:head>
	<title>AutoDroid Editor - {apk?.package_name || 'APK详情'}</title>
</svelte:head>

<div class="min-h-screen bg-gray-50">
	<!-- 头部组件 -->
	<Header activeTab="apks" onTabChange={handleTabChange} />

	<!-- 页面标题 -->
	<header class="bg-white shadow-sm border-b">
		<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
			<div class="flex justify-between items-center">
				<div>
					<h1 class="text-2xl font-bold text-gray-900">
						{apk?.package_name || 'APK详情'}
					</h1>
				</div>
				<div class="text-sm text-gray-500">编辑管理模块</div>
			</div>
		</div>
	</header>

	<!-- 主要内容 -->
	<main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
		<!-- 加载状态 -->
		{#if loading}
			<div class="flex justify-center items-center py-12">
				<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
			</div>
		{:else if error}
			<!-- 错误状态 -->
			<div class="bg-red-50 border border-red-200 rounded-lg p-4">
				<div class="flex">
					<div class="flex-shrink-0">
						<svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
							<path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
						</svg>
					</div>
					<div class="ml-3">
						<h3 class="text-sm font-medium text-red-800">加载失败</h3>
						<p class="text-sm text-red-700 mt-1">{error}</p>
					</div>
				</div>
			</div>
		{:else if apk}
			<!-- APK信息和操作记录 -->
			<div class="space-y-8">
				<!-- APK基本信息 -->
				<div class="bg-white rounded-lg shadow-sm border border-gray-200">
					<div class="p-6">
						<div class="grid grid-cols-1 md:grid-cols-4 gap-6">
							<div>
								<h3 class="text-sm font-medium text-gray-500">包名</h3>
								<p class="mt-1 text-lg font-semibold text-gray-900">{apk.package_name}</p>
							</div>
							<div>
								<h3 class="text-sm font-medium text-gray-500">版本</h3>
								<p class="mt-1 text-lg font-semibold text-green-600">v{apk.version}</p>
							</div>
							<div>
								<h3 class="text-sm font-medium text-gray-500">安装时间</h3>
								<p class="mt-1 text-lg font-semibold text-gray-900">{formatDate(apk.install_time)}</p>
							</div>
							<div>
								<h3 class="text-sm font-medium text-gray-500">最后操作</h3>
								<p class="mt-1 text-lg font-semibold text-blue-600">{formatTimeAgo(apk.last_operation_time)}</p>
							</div>
						</div>
						<div class="mt-6 grid grid-cols-1 md:grid-cols-3 gap-6">
							<div class="text-center">
								<div class="text-3xl font-bold text-blue-600">{apk.total_operations}</div>
								<div class="text-sm text-gray-500">操作记录</div>
							</div>
							<div class="text-center">
								<div class="text-3xl font-bold text-green-600">{apk.total_screenshots}</div>
								<div class="text-sm text-gray-500">截屏数量</div>
							</div>
							<div class="text-center">
								<div class="text-3xl font-bold text-purple-600">{operations.length}</div>
								<div class="text-sm text-gray-500">当前显示</div>
							</div>
						</div>
					</div>
				</div>

				<!-- 操作记录列表 -->
				<div class="bg-white rounded-lg shadow-sm border border-gray-200">
					<div class="p-6">
						<div class="flex justify-between items-center mb-6">
							<h2 class="text-xl font-semibold text-gray-900">
								用户操作记录
							</h2>
							<span class="text-sm text-gray-500">
								按时间倒序排列，共 {operations.length} 条记录
							</span>
						</div>

						{#if operations.length === 0}
							<div class="text-center py-12">
								<svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
								</svg>
								<h3 class="mt-2 text-sm font-medium text-gray-900">
									暂无操作记录
								</h3>
								<p class="mt-1 text-sm text-gray-500">
									该APK还没有用户操作记录
								</p>
							</div>
						{:else}
							<div class="space-y-4">
								{#each operations as operation, index}
									<div class="flex items-start space-x-4 p-4 border border-gray-200 rounded-lg hover:bg-gray-50">
										<!-- 序号 -->
										<div class="flex-shrink-0 w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center">
											<span class="text-sm font-medium text-gray-600">{operations.length - index}</span>
										</div>

										<!-- 操作图标 -->
										<div class="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center text-xl">
											{getActionTypeIcon(operation.action_type)}
										</div>

										<!-- 操作详情 -->
										<div class="flex-1 min-w-0">
											<div class="flex items-center space-x-2 mb-1">
												<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium {getActionTypeColor(operation.action_type)}">
													{operation.action_type.toUpperCase()}
												</span>
												<span class="text-sm text-gray-500">{formatTimeAgo(operation.timestamp)}</span>
												<span class="text-sm text-gray-400">({formatDate(operation.timestamp)})</span>
											</div>

											<div class="text-sm text-gray-700">
												{#if operation.target_element}
													<div>目标元素: <span class="font-medium">{operation.target_element}</span></div>
												{/if}
												{#if operation.target_text}
													<div>输入文本: <span class="font-medium">"{operation.target_text}"</span></div>
												{/if}
												{#if operation.coordinates}
													<div>坐标: ({operation.coordinates.x}, {operation.coordinates.y})</div>
												{/if}
												{#if operation.duration}
													<div>持续时间: {operation.duration}ms</div>
												{/if}
											</div>

											<!-- 截屏关联 -->
											<div class="mt-2 flex space-x-2">
												{#if operation.src_screenshot_id}
													<span class="inline-flex items-center px-2 py-1 rounded text-xs bg-blue-100 text-blue-800">
														源截屏: {operation.src_screenshot_id.slice(0, 8)}
													</span>
												{/if}
												{#if operation.dest_screenshot_id}
													<span class="inline-flex items-center px-2 py-1 rounded text-xs bg-green-100 text-green-800">
														目标截屏: {operation.dest_screenshot_id.slice(0, 8)}
													</span>
												{/if}
											</div>
										</div>

										<!-- 操作ID -->
										<div class="flex-shrink-0">
											<span class="text-xs text-gray-400">{operation.id.slice(0, 8)}</span>
										</div>
									</div>
								{/each}
							</div>
						{/if}
					</div>
				</div>
			</div>
		{/if}
	</main>
</div>