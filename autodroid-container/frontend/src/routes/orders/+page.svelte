<script lang="ts">
	import type { Order } from '$lib/types';
	import { onMount } from 'svelte';

	let orders: Order[] = [];
	let isLoading = true;
	let error: string | null = null;
	let selectedOrder: Order | null = null;
	let statusFilter: string = 'all';
	let searchQuery: string = '';

	async function fetchOrders() {
		isLoading = true;
		error = null;
		
		try {
			// For now, use a mock endpoint since the actual API might not exist yet
			// In a real implementation, this would be:
			// const response = await fetch('/api/orders');
			// const data = await response.json();
			// orders = data.orders || [];
			
			// Mock data for demonstration
			orders = [
				{
					id: 'order-1',
					name: 'Test Order 1',
					description: 'Test order for workflow 1 on device 1',
					workflow_name: 'workflow-1',
					device_udid: 'device-123',
					status: 'pending',
					created_at: '2025-01-01T10:00:00Z',
					updated_at: '2025-01-01T10:00:00Z',
					priority: 1
				},
				{
					id: 'order-2',
					name: 'Test Order 2',
					description: 'Test order for workflow 2 on device 2',
					workflow_name: 'workflow-2',
					device_udid: 'device-456',
					status: 'running',
					created_at: '2025-01-02T14:30:00Z',
					updated_at: '2025-01-02T14:30:00Z',
					priority: 2
				},
				{
					id: 'order-3',
					name: 'Test Order 3',
					description: 'Test order for workflow 1 on device 3',
					workflow_name: 'workflow-1',
					device_udid: 'device-789',
					status: 'completed',
					created_at: '2025-01-03T09:15:00Z',
					updated_at: '2025-01-03T09:20:45Z',
					priority: 1
				}
			];
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching orders:', err);
		} finally {
			isLoading = false;
		}
	}

	function getFilteredOrders() {
		return orders.filter(order => {
			// Status filter
			const matchesStatus = statusFilter === 'all' || order.status === statusFilter;
			
			// Search query filter
			const matchesSearch = !searchQuery || 
				order.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
				order.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
				order.workflow_name.toLowerCase().includes(searchQuery.toLowerCase());
			
			return matchesStatus && matchesSearch;
		});
	}

	function selectOrder(order: Order) {
		selectedOrder = order;
	}

	function closeOrderDetails() {
		selectedOrder = null;
	}

	// Fetch orders on component mount
	onMount(fetchOrders);
</script>

<div class="orders-page">
	<h1>Orders</h1>
	
	{#if isLoading}
		<div class="loading">Loading orders...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="orders-container">
			<!-- Orders List -->
			<div class="orders-list">
				<div class="orders-header">
					<h2>Order List</h2>
					<div class="filters">
						<div class="search-box">
							<input 
								type="text" 
								placeholder="Search orders..." 
								bind:value={searchQuery}
								class="search-input"
							/>
						</div>
						<div class="status-filter">
							<select bind:value={statusFilter} class="filter-select">
								<option value="all">All Status</option>
								<option value="pending">Pending</option>
								<option value="in_progress">In Progress</option>
								<option value="completed">Completed</option>
								<option value="failed">Failed</option>
							</select>
						</div>
					</div>
				</div>
				
				{#if getFilteredOrders().length === 0}
					<div class="no-orders">No orders found</div>
				{:else}
					<div class="order-cards">
						{#each getFilteredOrders() as order}
							<div class="order-card" on:click={() => selectOrder(order)} on:keydown={(e) => { if (e.key === 'Enter' || e.key === ' ') selectOrder(order); }} role="button" tabindex="0">
								<div class="order-header">
									<h3>{order.name}</h3>
									<span class={`status-badge ${order.status}`}>
										{order.status.charAt(0).toUpperCase() + order.status.slice(1)}
									</span>
								</div>
								<p class="description">{order.description || 'No description'}</p>
								<div class="order-meta">
									<div class="meta-item">
										<span class="label">Workflow:</span>
										<span class="value">{order.workflow_name}</span>
									</div>
									<div class="meta-item">
										<span class="label">Device:</span>
										<span class="value">{order.device_udid}</span>
									</div>
									<div class="meta-item">
										<span class="label">Priority:</span>
										<span class="value">{order.priority}</span>
									</div>
									<div class="meta-item">
										<span class="label">Created:</span>
										<span class="value">{new Date(order.created_at).toLocaleString()}</span>
									</div>
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			
			<!-- Order Details -->
			{#if selectedOrder}
				<div class="order-details">
					<div class="details-header">
						<h2>Order Details</h2>
						<button class="close-button" on:click={closeOrderDetails}>×</button>
					</div>
					
					<div class="details-content">
						<div class="order-info">
							<h3>{selectedOrder.name}</h3>
							<p class="description">{selectedOrder.description || 'No description'}</p>
							<div class="order-status">
								<span class={`status-badge ${selectedOrder.status}`}>
									{selectedOrder.status.charAt(0).toUpperCase() + selectedOrder.status.slice(1)}
								</span>
								<span class="priority">Priority: {selectedOrder.priority}</span>
							</div>
						</div>
						
						<div class="order-metadata">
							<h4>Metadata</h4>
							<div class="metadata-grid">
								<div class="metadata-item">
									<span class="meta-key">Workflow:</span>
									<span class="meta-value">{selectedOrder.workflow_name}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Device UDID:</span>
									<span class="meta-value">{selectedOrder.device_udid}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Created At:</span>
									<span class="meta-value">{new Date(selectedOrder.created_at).toLocaleString()}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Updated At:</span>
									<span class="meta-value">{new Date(selectedOrder.updated_at).toLocaleString()}</span>
								</div>
							</div>
						</div>
						
						<div class="order-actions">
							<button class="primary-button">Edit Order</button>
							<button class="secondary-button">Delete Order</button>
							<button class="secondary-button">Cancel Order</button>
						</div>
					</div>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.orders-page {
		max-width: 1200px;
		margin: 0 auto;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 2rem;
		font-size: 2rem;
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

	/* Orders Container */
	.orders-container {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 2rem;
	}

	/* Orders List */
	.orders-list {
		grid-column: 1;
	}

	.orders-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 1.5rem;
	}

	.orders-header h2 {
		color: #2c3e50;
		margin: 0;
		font-size: 1.25rem;
	}

	/* Filters */
	.filters {
		display: flex;
		gap: 1rem;
		align-items: center;
	}

	.search-box {
		flex: 1;
		max-width: 300px;
	}

	.search-input {
		width: 100%;
		padding: 0.75rem;
		border: 1px solid #ddd;
		border-radius: 6px;
		font-size: 0.9rem;
		transition: border-color 0.2s ease;
	}

	.search-input:focus {
		outline: none;
		border-color: #3498db;
	}

	.status-filter {
		min-width: 150px;
	}

	.filter-select {
		width: 100%;
		padding: 0.75rem;
		border: 1px solid #ddd;
		border-radius: 6px;
		font-size: 0.9rem;
		background-color: white;
		transition: border-color 0.2s ease;
	}

	.filter-select:focus {
		outline: none;
		border-color: #3498db;
	}

	.no-orders {
		text-align: center;
		padding: 2rem;
		background-color: #f8f9fa;
		border-radius: 8px;
		color: #6c757d;
	}

	.order-cards {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.order-card {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		cursor: pointer;
		transition: all 0.2s ease;
		border: 2px solid transparent;
	}

	.order-card:hover {
		transform: translateY(-2px);
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
		border-color: #3498db;
	}

	.order-header {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-bottom: 0.5rem;
	}

	.order-header h3 {
		color: #2c3e50;
		margin: 0;
		font-size: 1.1rem;
		flex: 1;
		margin-right: 1rem;
	}

	.status-badge {
		padding: 0.25rem 0.75rem;
		border-radius: 12px;
		font-size: 0.8rem;
		font-weight: 500;
		align-self: flex-start;
	}

	.status-badge.pending {
		background-color: #e3f2fd;
		color: #1976d2;
	}

	.status-badge.in_progress {
		background-color: #fff3e0;
		color: #f39c12;
	}

	.status-badge.completed {
		background-color: #e8f5e8;
		color: #27ae60;
	}

	.status-badge.failed {
		background-color: #ffebee;
		color: #d32f2f;
	}

	.order-card .description {
		color: #6c757d;
		margin-bottom: 1rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.order-meta {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.meta-item {
		display: flex;
		justify-content: space-between;
		font-size: 0.85rem;
	}

	.meta-item .label {
		font-weight: 500;
		color: #6c757d;
	}

	.meta-item .value {
		font-weight: 600;
		color: #2c3e50;
	}

	/* Order Details */
	.order-details {
		grid-column: 2;
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		height: fit-content;
		position: sticky;
		top: 2rem;
	}

	.details-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 1.5rem;
	}

	.details-header h2 {
		color: #2c3e50;
		margin: 0;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
		flex: 1;
	}

	.close-button {
		background: none;
		border: none;
		font-size: 1.5rem;
		cursor: pointer;
		color: #6c757d;
		padding: 0;
		width: 30px;
		height: 30px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 4px;
		transition: background-color 0.2s ease;
	}

	.close-button:hover {
		background-color: #f0f0f0;
	}

	/* Order Info */
	.order-info h3 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 0.5rem;
		font-size: 1.1rem;
	}

	.order-info .description {
		color: #6c757d;
		margin-bottom: 1rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.order-status {
		display: flex;
		align-items: center;
		gap: 1rem;
		margin-bottom: 1.5rem;
	}

	.order-status .priority {
		font-size: 0.9rem;
		color: #6c757d;
	}

	/* Order Metadata */
	.order-metadata h4 {
		color: #34495e;
		margin-top: 0;
		margin-bottom: 1rem;
		font-size: 1rem;
	}

	.metadata-grid {
		display: grid;
		grid-template-columns: 1fr;
		gap: 0.75rem;
		margin-bottom: 2rem;
		padding: 1rem;
		background-color: #f8f9fa;
		border-radius: 6px;
	}

	.metadata-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.meta-key {
		font-weight: 500;
		color: #6c757d;
	}

	.meta-value {
		font-weight: 600;
		color: #2c3e50;
	}

	/* Order Actions */
	.order-actions {
		display: flex;
		gap: 1rem;
		justify-content: flex-end;
		padding-top: 1rem;
		border-top: 1px solid #ecf0f1;
	}

	.primary-button {
		background-color: #3498db;
		color: white;
		border: none;
		padding: 0.75rem 1.5rem;
		border-radius: 6px;
		cursor: pointer;
		font-weight: 500;
		transition: background-color 0.2s ease;
	}

	.primary-button:hover {
		background-color: #2980b9;
	}

	.secondary-button {
		background-color: #f0f0f0;
		color: #555;
		border: 1px solid #ddd;
		padding: 0.75rem 1.5rem;
		border-radius: 6px;
		cursor: pointer;
		font-weight: 500;
		transition: all 0.2s ease;
	}

	.secondary-button:hover {
		background-color: #e0e0e0;
		border-color: #ccc;
	}

	/* Responsive Design */
	@media (max-width: 1024px) {
		.orders-container {
			grid-template-columns: 1fr;
		}
		
		.order-details {
			position: static;
		}
		
		.orders-header {
			flex-direction: column;
			align-items: flex-start;
			gap: 1rem;
		}
		
		.filters {
			width: 100%;
		}
	}

	@media (max-width: 640px) {
		.filters {
			flex-direction: column;
			align-items: stretch;
		}
		
		.search-box {
			max-width: none;
		}
		
		.order-actions {
			flex-direction: column;
		}
		
		.order-actions button {
			width: 100%;
		}
	}
</style>