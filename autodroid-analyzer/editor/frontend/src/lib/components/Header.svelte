<script lang="ts">
	export let activeTab: "devices" | "apks" = "apks";
	export let onTabChange: (tab: "devices" | "apks") => void;
</script>

<header class="bg-white shadow-sm border-b">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
		<!-- Logo和标题区域 -->
		<div class="py-4 border-b border-gray-200">
			<div class="flex justify-between items-center">
				<h1 class="text-2xl font-bold text-gray-900">
					AutoDroid Editor
				</h1>
				<div class="text-sm text-gray-500">编辑管理模块</div>
			</div>
		</div>

		<!-- Tab切换菜单 -->
		<div class="py-2">
			<nav class="-mb-px flex space-x-8">
				<button
					class="py-2 px-1 border-b-2 font-medium text-sm {activeTab ===
					'devices'
						? 'border-blue-500 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => onTabChange("devices")}
				>
					Device列表
				</button>
				<button
					class="py-2 px-1 border-b-2 font-medium text-sm {activeTab ===
					'apks'
						? 'border-blue-500 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => onTabChange("apks")}
				>
					APK应用列表
				</button>
			</nav>
		</div>
	</div>
</header>
