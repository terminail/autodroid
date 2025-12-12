<script lang="ts">
	let props = $props();
	let activeTab = props.activeTab ?? "apks";
	let onTabChange = props.onTabChange ?? ((tab: "devices" | "apks") => {});

	// Ensure props are not undefined for HMR/hydration
	if (typeof activeTab !== "string") activeTab = "apks";
	if (typeof onTabChange !== "function")
		onTabChange = (tab: "devices" | "apks") => {};
</script>

<nav class="navbar">
	<div class="navbar-brand">
		<h1>AutoDroid Editor</h1>
	</div>
	<div class="navbar-menu">
		<a href="/device" class:active={activeTab === "devices"}>
			Device列表
		</a>
		<a href="/apk" class:active={activeTab === "apks"}> APK应用列表 </a>
	</div>
</nav>
