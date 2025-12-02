<script lang="ts">
	import { onMount } from 'svelte';

	let isLoading = true;
	let error: string = '';
	let serverData = {
		name: 'Loading...',
		hostname: 'Loading...',
		ip_address: 'Loading...',
		platform: 'Loading...'
	};

	async function fetchServerData() {
		try {
			const response = await fetch('http://localhost:8003/api/server');
			if (!response.ok) {
				throw new Error('Failed to fetch server info');
			}
			const data = await response.json();
			
			// 防御性处理：确保返回的数据结构正确
					serverData = {
						name: data?.name || 'Unknown',
						hostname: data?.hostname || 'Unknown',
						ip_address: data?.ip_address || 'Unknown',
						platform: data?.platform || 'Unknown',
						api_base_url: data?.api_base_url || `http://${data?.ip_address || 'localhost'}:8003/api`
					};
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching server data:', err);
		} finally {
			isLoading = false;
		}
	}

	// Fetch data on component mount (client-side only)
	onMount(fetchServerData);
</script>

<main class="dashboard">
	{#if isLoading}
		<div class="loading">Loading dashboard...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="dashboard-grid">
			<!-- Connection Status -->
			<div class="dashboard-section">
					<h2>Connection Status</h2>
					<div class="status-grid">
						<div class="status-item">
							<span class="status-label">Server Name:</span>
							<span class="status-value">{serverData.name}</span>
						</div>
						<div class="status-item">
							<span class="status-label">Hostname:</span>
							<span class="status-value">{serverData.hostname}</span>
						</div>
						<div class="status-item">
							<span class="status-label">IP Address:</span>
							<span class="status-value">{serverData.ip_address}</span>
						</div>
						<div class="status-item">
							<span class="status-label">Platform:</span>
							<span class="status-value">{serverData.platform}</span>
						</div>
						<div class="status-item">
						<span class="status-label">API Endpoint:</span>
					<span class="status-value">{serverData.api_base_url || 'http://localhost:8003/api'}</span>
					</div>
					</div>
				</div>

			<!-- Simple Status Message -->
			<div class="dashboard-section">
				<h2>Dashboard Status</h2>
				<p>Dashboard is working correctly. Full functionality will be restored in the next update.</p>
			</div>
		</div>
	{/if}
</main>

<style>
		.dashboard {
			max-width: 1200px;
			margin: 0 auto;
		}

		.loading, .error {
			text-align: center;
			padding: 2rem;
			border-radius: 8px;
			margin: 2rem 0;
		}

		.loading {
			background-color: #e3f2fd;
			color: #1976d2;
		}

		.error {
			background-color: #ffebee;
			color: #d32f2f;
		}

		.dashboard-grid {
			display: grid;
			grid-template-columns: 1fr 300px;
			gap: 2rem;
		}

		.dashboard-section {
			background: white;
			border-radius: 8px;
			padding: 1.5rem;
			box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		}

		.dashboard-section h2 {
			color: #2c3e50;
			margin-top: 0;
			margin-bottom: 1.5rem;
			font-size: 1.25rem;
			border-bottom: 2px solid #ecf0f1;
			padding-bottom: 0.5rem;
		}

		.status-grid {
			display: flex;
			flex-direction: column;
			gap: 1rem;
		}

		.status-item {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 0.75rem;
			background-color: #f8f9fa;
			border-radius: 6px;
		}

		.status-label {
			font-weight: 500;
			color: #6c757d;
		}

		.status-value {
			font-weight: 600;
			color: #2c3e50;
		}

		/* Responsive Design */
		@media (max-width: 1024px) {
			.dashboard-grid {
				grid-template-columns: 1fr;
			}
		}
	</style>