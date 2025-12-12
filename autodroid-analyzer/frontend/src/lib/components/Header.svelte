<script lang="ts">
	let props = $props();
	let activeTab = props.activeTab ?? "apks";
	let onTabChange = props.onTabChange ?? ((tab: "devices" | "apks") => {});
	
	// Ensure props are not undefined for HMR/hydration
	if (typeof activeTab !== 'string') activeTab = "apks";
	if (typeof onTabChange !== 'function') onTabChange = (tab: "devices" | "apks") => {};
</script>

<header class="bg-white shadow-sm border-b">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
		<div class="flex justify-between items-center py-4">
			<div class="flex items-center space-x-4">
				<h1 class="text-2xl font-bold text-gray-900">
					AutoDroid Editor
				</h1>
				<div class="text-sm text-gray-500">编辑管理模块</div>
			</div>

			<nav class="-mb-px flex space-x-8">
				<a
					href="/device"
					class="py-2 px-1 border-b-2 font-medium text-sm {activeTab === 'devices' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
				>
					Device列表
				</a>
				<a
					href="/apk"
					class="py-2 px-1 border-b-2 font-medium text-sm {activeTab === 'apks' ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
				>
					APK应用列表
				</a>
			</nav>
		</div>
	</div>
</header>
