<script lang="ts">
	import { goto } from "$app/navigation";
	import Header from "$lib/components/Header.svelte";

	// 处理Tab切换
	function handleTabChange(tab: "devices" | "apks") {
		if (tab === "apks") {
			goto("/apk");
		}
	}
</script>

<div class="min-h-screen bg-gray-50">
	<!-- 头部组件 -->
	<Header activeTab="devices" onTabChange={handleTabChange} />

	<!-- 主要内容 -->
	<main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
		<div class="space-y-6">
			<h2 class="text-xl font-semibold text-gray-900">Device列表</h2>
			<div
				class="bg-white shadow-sm rounded-lg border border-gray-200 p-6 text-center"
			>
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
						d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z"
					/>
				</svg>
				<h3 class="mt-2 text-sm font-medium text-gray-900">
					Device列表功能开发中
				</h3>
				<p class="mt-1 text-sm text-gray-500">
					正在实现Device列表功能，将显示deviceId、name、连接状态等信息
				</p>
			</div>
		</div>
	</main>
</div>
