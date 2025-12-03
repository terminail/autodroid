<script lang="ts">
	import { onMount } from 'svelte';
	import type { ServerData } from '$lib/types';

	let isLoading = true;
	let error: string = '';
	let serverData: ServerData = {
		name: 'Loading...',
		hostname: 'Loading...',
		ip_address: 'Loading...',
		platform: 'Loading...'
	};
	let qrCodeUrl = '';

	async function fetchServerData() {
		try {
			// 获取服务器信息
			const serverResponse = await fetch('/api/server');
			if (!serverResponse.ok) {
				throw new Error('Failed to fetch server info');
			}
			const data = await serverResponse.json();
			
			// 防御性处理：确保返回的数据结构正确
			serverData = {
				name: data?.name || 'Unknown',
				hostname: data?.hostname || 'Unknown',
				ip_address: data?.ip_address || 'Unknown',
				platform: data?.platform || 'Unknown',
				api_base_url: data?.api_base_url || `/api`
			};
			
			// 获取二维码图片URL
			qrCodeUrl = '/api/qr-code';
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
						<!-- QR Code Image -->
						<div class="qr-image-container">
							<img src={qrCodeUrl} alt="Server QR Code" />
						</div>
						
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
					<span class="status-value">{serverData.api_base_url || '/api'}</span>
					</div>
					</div>
				</div>

			<!-- Simple Status Message -->
			<div class="dashboard-section full-width">
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
		grid-template-columns: 1fr;
		gap: 2rem;
	}

	.full-width {
		grid-column: 1 / -1;
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

	.qr-image-container {
		display: flex;
		justify-content: center;
		margin-bottom: 1.5rem;
	}

	.qr-image-container img {
		max-width: 200px;
		border: 1px solid #e0e0e0;
		border-radius: 6px;
		padding: 0.5rem;
		background-color: #f9f9f9;
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