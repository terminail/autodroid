<script lang="ts">
	import { api } from "$lib/api";
	import Header from "$lib/components/Header.svelte";
	import type { ApkInfo } from "$lib/types";
	import { onMount } from "svelte";

	// APK相关状态
	let apks: ApkInfo[] = [];
	let loading = true;
	let error: string | null = null;

	// 分页相关状态
	let currentPage = 1;
	let itemsPerPage = 10;
	let totalPages = 0;

	onMount(async () => {
		const response = await api.getApks();
		if (response.success) {
			apks = response.data || [];
			totalPages = Math.ceil(apks.length / itemsPerPage);
		} else {
			error = response.error || "获取APK列表失败";
		}
		loading = false;
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

	// 获取当前页的数据
	function getCurrentPageData(): ApkInfo[] {
		const startIndex = (currentPage - 1) * itemsPerPage;
		const endIndex = startIndex + itemsPerPage;
		return apks.slice(startIndex, endIndex);
	}

	// 分页控制函数
	function goToPage(page: number) {
		if (page >= 1 && page <= totalPages) {
			currentPage = page;
		}
	}

	function nextPage() {
		if (currentPage < totalPages) {
			currentPage++;
		}
	}

	function prevPage() {
		if (currentPage > 1) {
			currentPage--;
		}
	}
</script>

<!-- 头部组件 -->
<Header activeTab="apks" />

<!-- 主要内容 -->
<div>
		<!-- 加载状态 -->
		{#if loading}
			<div class="flex justify-center items-center py-12">
				<div
					class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"
				></div>
			</div>
		{:else if error}
			<!-- 错误状态 -->
			<div class="bg-red-50 border border-red-200 rounded-lg p-4">
				<div class="flex">
					<div class="flex-shrink-0">
						<svg
							class="h-5 w-5 text-red-400"
							viewBox="0 0 20 20"
							fill="currentColor"
						>
							<path
								fill-rule="evenodd"
								d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
								clip-rule="evenodd"
							/>
						</svg>
					</div>
					<div class="ml-3">
						<h3 class="text-sm font-medium text-red-800">
							加载失败
						</h3>
						<p class="text-sm text-red-700 mt-1">{error}</p>
					</div>
				</div>
			</div>
		{:else}
			<div class="space-y-6">
				<div class="flex justify-between items-center">
					<h2 class="text-xl font-semibold text-gray-900">
						APK应用列表
					</h2>
					<span class="text-sm text-gray-500">
						共 {apks.length} 个应用，第 {currentPage} 页，共 {totalPages}
						页
					</span>
				</div>
			</div>
		{/if}
		{#if apks.length === 0}
			<div class="text-center py-12">
				<svg
					class="mx-auto h-12 w-12 text-gray-400"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
					/>
				</svg>
				<h3 class="mt-2 text-sm font-medium text-gray-900">
					暂无APK记录
				</h3>
				<p class="mt-1 text-sm text-gray-500">
					请先运行用户监控模块来创建APK操作记录
				</p>
			</div>
		{:else}
			<!-- 表格容器 -->
			<div class="bg-white shadow-sm rounded-lg border border-gray-200">
				<!-- 表格头部 -->
				<div class="overflow-x-auto">
					<table class="min-w-full divide-y divide-gray-200">
						<thead class="bg-gray-50">
							<tr>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									ID
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									应用名称
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									版本名称
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									版本代码
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									安装时间
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									操作记录
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									截屏数量
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									最后分析
								</th>
								<th
									class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
								>
									操作
								</th>
							</tr>
						</thead>
						<tbody class="bg-white divide-y divide-gray-200">
							{#each getCurrentPageData() as apk}
								<tr class="hover:bg-gray-50">
									<td
										class="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900"
									>
										{apk.id}
									</td>
									<td class="px-6 py-4 whitespace-nowrap">
										<div
											class="text-sm font-medium text-gray-900"
										>
											{apk.app_name}
										</div>
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-gray-700"
									>
										{apk.version_name}
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"
									>
										{apk.version_code || "-"}
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"
									>
										{formatDate(apk.install_time)}
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-gray-900"
									>
										{apk.total_operations} 条
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-gray-900"
									>
										{apk.total_screenshots} 张
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm text-blue-600"
									>
										{apk.last_analyzed
											? formatTimeAgo(apk.last_analyzed)
											: "未分析"}
									</td>
									<td
										class="px-6 py-4 whitespace-nowrap text-sm font-medium"
									>
										<a
											href="/apk/{apk.id}"
											class="text-blue-600 hover:text-blue-900"
										>
											查看详情
										</a>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			</div>

			<!-- 分页控件 -->
			{#if totalPages > 1}
				<div class="px-6 py-4 bg-gray-50 border-t border-gray-200">
					<div class="flex items-center justify-between">
						<div class="flex items-center space-x-2">
							<span class="text-sm text-gray-700">
								每页显示
							</span>
							<select
								class="text-sm border border-gray-300 rounded px-2 py-1"
								bind:value={itemsPerPage}
								on:change={() => {
									totalPages = Math.ceil(
										apks.length / itemsPerPage,
									);
									currentPage = 1;
								}}
							>
								<option value="5">5</option>
								<option value="10">10</option>
								<option value="20">20</option>
								<option value="50">50</option>
							</select>
							<span class="text-sm text-gray-700"> 条记录 </span>
						</div>

						<div class="flex items-center space-x-2">
							<button
								class="px-3 py-1 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
								on:click={prevPage}
								disabled={currentPage === 1}
							>
								上一页
							</button>

							<div class="flex items-center space-x-1">
								{#each Array(totalPages) as _, i}
									{#if i + 1 === currentPage || i + 1 === currentPage - 1 || i + 1 === currentPage + 1 || i + 1 === 1 || i + 1 === totalPages}
										{#if (i + 1 === currentPage - 1 && currentPage > 2) || (i + 1 === currentPage + 1 && currentPage < totalPages - 1)}
											<span
												class="px-2 text-sm text-gray-500"
												>...</span
											>
										{:else}
											<button
												class="px-3 py-1 text-sm border rounded {i +
													1 ===
												currentPage
													? 'bg-blue-600 text-white border-blue-600'
													: 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'}"
												on:click={() => goToPage(i + 1)}
											>
												{i + 1}
											</button>
										{/if}
									{/if}
								{/each}
							</div>

							<button
								class="px-3 py-1 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
								on:click={nextPage}
								disabled={currentPage === totalPages}
							>
								下一页
							</button>
						</div>
					</div>
				</div>
			{/if}
		{/if}
	</div>
