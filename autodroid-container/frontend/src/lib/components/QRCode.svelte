<script lang="ts">
	import { onMount } from 'svelte';

	export let apiUrl = '/api';
	
	let qrCodeUrl = '';
	let isLoading = true;
	let error: string = '';
	let qrData = {
		server_name: '',
		apiEndpoint: '',
		ipAddress: '',
		port: 0,
		protocol: '',
		version: '',
		timestamp: '',
		expiry: ''
	};

	onMount(async () => {
		try {
			// 获取二维码数据
			const response = await fetch(`${apiUrl}/qr-code/data`);
			if (!response.ok) {
				throw new Error('Failed to fetch QR code data');
			}
			const data = await response.json();
			qrData = data.qr_data;
			
			// 生成二维码图片URL
			qrCodeUrl = `${apiUrl}/qr-code`;
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching QR code:', err);
		} finally {
			isLoading = false;
		}
	});

	function formatDate(dateString: string) {
		if (!dateString) return '';
		const date = new Date(dateString);
		return date.toLocaleString();
	}
</script>

<div class="qr-code-container">
	<h3>Server QR Code</h3>
	{#if isLoading}
		<div class="loading">Loading QR code...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="qr-content">
			<div class="qr-image">
				<img src={qrCodeUrl} alt="Server QR Code" />
			</div>
			<div class="qr-info">
				<div class="info-item">
					<span class="info-label">Server:</span>
					<span class="info-value">{qrData.server_name}</span>
				</div>
				<div class="info-item">
					<span class="info-label">Endpoint:</span>
					<span class="info-value">{qrData.apiEndpoint}</span>
				</div>
				<div class="info-item">
					<span class="info-label">IP Address:</span>
					<span class="info-value">{qrData.ipAddress}:{qrData.port}</span>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	.qr-code-container {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		height: 100%;
	}

	.qr-code-container h3 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 1.5rem;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
	}

	.loading, .error {
		text-align: center;
		padding: 1rem;
		border-radius: 6px;
		margin: 1rem 0;
	}

	.loading {
		background-color: #e3f2fd;
		color: #1976d2;
	}

	.error {
		background-color: #ffebee;
		color: #d32f2f;
	}

	.qr-content {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.qr-image {
		display: flex;
		justify-content: center;
		margin-bottom: 1rem;
	}

	.qr-image img {
		max-width: 200px;
		border: 1px solid #e0e0e0;
		border-radius: 6px;
		padding: 0.5rem;
		background-color: #f9f9f9;
	}

	.qr-info {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.info-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0.5rem;
		background-color: #f8f9fa;
		border-radius: 4px;
	}

	.info-label {
		font-weight: 500;
		color: #6c757d;
		font-size: 0.9rem;
	}

	.info-value {
		font-weight: 600;
		color: #2c3e50;
		font-size: 0.9rem;
		text-align: right;
		word-break: break-all;
	}

	/* Responsive Design */
	@media (max-width: 768px) {
		.qr-image img {
			max-width: 150px;
		}
		
		.info-item {
			flex-direction: column;
			align-items: flex-start;
			gap: 0.25rem;
		}
		
		.info-value {
			text-align: left;
		}
	}
</style>