<script lang="ts">
	let { children } = $props();
</script>

<div class="auth-layout">
	{@render children()}
</div>

<style>
	.auth-layout {
		min-height: 100vh;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	}
</style>