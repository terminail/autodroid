<script lang="ts">
	import Header from "$lib/components/Header.svelte";
</script>

<svelte:head>
	<title>AutoDroid Editor - 会话管理</title>
</svelte:head>

<div class="min-h-screen bg-gray-50">
	<!-- 头部组件 -->
	<Header activeTab="apks" />

	<!-- 主要内容 -->
	<main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
		<div class="text-center py-12">
			<svg
				class="mx-auto h-16 w-16 text-gray-400"
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
			<h2 class="mt-4 text-xl font-semibold text-gray-900">
				欢迎使用 AutoDroid Editor
			</h2>
			<p class="mt-2 text-sm text-gray-500">
				请选择上方的标签页来管理设备或APK应用
			</p>
		</div>
	</main>
</div>
