<script lang="ts">
	import "../app.css";

	let { children } = $props();
</script>

<main class="main-content">
	{@render children()}
</main>
