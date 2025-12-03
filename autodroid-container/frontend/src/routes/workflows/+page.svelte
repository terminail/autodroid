<script lang="ts">
	import type { WorkflowInfo } from '$lib/types';
	import { onMount } from 'svelte';
	import { getApiUrl } from '$lib/config';

	let workflows: WorkflowInfo[] = [];
	let isLoading = true;
	let error: string | null = null;
	let selectedWorkflow: WorkflowInfo | null = null;

	async function fetchWorkflows() {
		isLoading = true;
		error = null;
		
		try {
			const apiUrl = getApiUrl('/workflows');
			const response = await fetch(apiUrl);
			if (!response.ok) {
				throw new Error('Failed to fetch workflows');
			}
			const data = await response.json();
			workflows = data.workflows || [];
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching workflows:', err);
		} finally {
			isLoading = false;
		}
	}

	async function fetchWorkflowDetails(workflowId: string) {
		isLoading = true;
		error = null;
		
		try {
			const apiUrl = getApiUrl(`/workflows/${workflowId}`);
			const response = await fetch(apiUrl);
			if (!response.ok) {
				throw new Error(`Failed to fetch workflow: ${workflowId}`);
			}
			selectedWorkflow = await response.json();
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error(`Error fetching workflow ${workflowId}:`, err);
		} finally {
			isLoading = false;
		}
	}

	function closeWorkflowDetails() {
		selectedWorkflow = null;
	}

	// Fetch workflows on component mount
	onMount(fetchWorkflows);
</script>

<div class="workflows-page">
	<h1>Workflows</h1>

	{#if isLoading}
		<div class="loading">Loading workflows...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="workflows-container">
			<!-- Workflows List -->
			<div class="workflows-list">
				<h2>Workflow List</h2>

				{#if workflows.length === 0}
					<div class="no-workflows">No workflows found</div>
				{:else}
					<div class="workflow-cards">
						{#each workflows as workflow}
							<div
								class="workflow-card"
								on:click={() =>
									fetchWorkflowDetails(workflow.id)}
								on:keydown={(e) => {
									if (e.key === "Enter" || e.key === " ")
										fetchWorkflowDetails(workflow.id);
								}}
								role="button"
								tabindex="0"
							>
								<h3>{workflow.name}</h3>
								<p class="description">
									{workflow.description || "No description"}
								</p>
								<div class="metadata">
									{#if workflow.metadata}
										{#each Object.entries(workflow.metadata) as [key, value]}
											<span class="meta-item"
												>{key}: {value}</span
											>
										{/each}
									{:else}
										<span class="no-metadata"
											>No metadata</span
										>
									{/if}
								</div>
								<div class="workflow-stats">
									<span class="step-count"
										>{workflow.steps.length} steps</span
									>
									{#if workflow.schedule}
										<span class="scheduled">Scheduled</span>
									{/if}
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>

			<!-- Workflow Details -->
			{#if selectedWorkflow}
				<div class="workflow-details">
					<div class="details-header">
						<h2>Workflow Details</h2>
						<button
							class="close-button"
							on:click={closeWorkflowDetails}>×</button
						>
					</div>

					<div class="details-content">
						<div class="workflow-header-info">
							<h3>{selectedWorkflow.name}</h3>
							<p class="description">
								{selectedWorkflow.description ||
									"No description"}
							</p>
						</div>

						<div class="workflow-metadata">
							<h4>Metadata</h4>
							{#if selectedWorkflow.metadata}
								<div class="metadata-grid">
									{#each Object.entries(selectedWorkflow.metadata) as [key, value]}
										<div class="metadata-item">
											<span class="meta-key">{key}</span>
											<span class="meta-value"
												>{value}</span
											>
										</div>
									{/each}
								</div>
							{:else}
								<div class="no-metadata">
									No metadata available
								</div>
							{/if}
						</div>

						<div class="workflow-steps">
							<h4>Steps ({selectedWorkflow.steps.length})</h4>
							<div class="steps-list">
								{#each selectedWorkflow.steps as step, index}
									<div class="step-item">
										<div class="step-header">
											<span class="step-number"
												>{index + 1}</span
											>
											<span class="step-type"
												>{step.type}</span
											>
											<span class="step-action"
												>{step.action}</span
											>
										</div>
										<div class="step-details">
											{#if step.selector}
												<div class="step-property">
													<span class="property-label"
														>Selector:</span
													>
													<span class="property-value"
														>{step.selector}</span
													>
												</div>
											{/if}
											{#if step.value !== undefined}
												<div class="step-property">
													<span class="property-label"
														>Value:</span
													>
													<span class="property-value"
														>{JSON.stringify(
															step.value,
														)}</span
													>
												</div>
											{/if}
											{#if step.timeout}
												<div class="step-property">
													<span class="property-label"
														>Timeout:</span
													>
													<span class="property-value"
														>{step.timeout}ms</span
													>
												</div>
											{/if}
										</div>
									</div>
								{/each}
							</div>
						</div>

						<div class="workflow-actions">
							<button class="primary-button">Edit Workflow</button
							>
							<button class="secondary-button"
								>Delete Workflow</button
							>
							<button class="secondary-button">Execute Now</button
							>
						</div>
					</div>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.workflows-page {
		max-width: 1200px;
		margin: 0 auto;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 2rem;
		font-size: 2rem;
	}

	.loading,
	.error {
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

	/* Workflows Container */
	.workflows-container {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 2rem;
	}

	/* Workflows List */
	.workflows-list {
		grid-column: 1;
	}

	.workflows-list h2 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 1.5rem;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
	}

	.no-workflows {
		text-align: center;
		padding: 2rem;
		background-color: #f8f9fa;
		border-radius: 8px;
		color: #6c757d;
	}

	.workflow-cards {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.workflow-card {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		cursor: pointer;
		transition: all 0.2s ease;
		border: 2px solid transparent;
	}

	.workflow-card:hover {
		transform: translateY(-2px);
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
		border-color: #3498db;
	}

	.workflow-card h3 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 0.5rem;
		font-size: 1.1rem;
	}

	.workflow-card .description {
		color: #6c757d;
		margin-bottom: 1rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.metadata {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		margin-bottom: 1rem;
	}

	.meta-item {
		background-color: #f0f0f0;
		padding: 0.25rem 0.5rem;
		border-radius: 4px;
		font-size: 0.8rem;
		color: #555;
	}

	.no-metadata {
		color: #999;
		font-size: 0.8rem;
	}

	.workflow-stats {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 1rem;
		padding-top: 1rem;
		border-top: 1px solid #ecf0f1;
	}

	.step-count {
		font-size: 0.85rem;
		color: #6c757d;
	}

	.scheduled {
		background-color: #e8f5e8;
		color: #27ae60;
		padding: 0.25rem 0.75rem;
		border-radius: 12px;
		font-size: 0.8rem;
		font-weight: 500;
	}

	/* Workflow Details */
	.workflow-details {
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

	.workflow-header-info h3 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 0.5rem;
		font-size: 1.1rem;
	}

	.workflow-header-info .description {
		color: #6c757d;
		margin-bottom: 1.5rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.workflow-metadata h4,
	.workflow-steps h4 {
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

	.no-metadata {
		color: #999;
		font-size: 0.9rem;
		text-align: center;
		padding: 1rem;
		background-color: #f8f9fa;
		border-radius: 6px;
	}

	/* Steps List */
	.steps-list {
		max-height: 400px;
		overflow-y: auto;
		margin-bottom: 2rem;
	}

	.step-item {
		margin-bottom: 1rem;
		padding: 1rem;
		background-color: #f8f9fa;
		border-radius: 6px;
		border-left: 4px solid #3498db;
	}

	.step-header {
		display: flex;
		gap: 0.75rem;
		align-items: center;
		margin-bottom: 0.75rem;
	}

	.step-number {
		background-color: #3498db;
		color: white;
		width: 24px;
		height: 24px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		font-size: 0.8rem;
		font-weight: 600;
	}

	.step-type {
		background-color: #e3f2fd;
		color: #1976d2;
		padding: 0.25rem 0.5rem;
		border-radius: 4px;
		font-size: 0.8rem;
		font-weight: 500;
	}

	.step-action {
		font-weight: 600;
		color: #2c3e50;
		font-size: 0.9rem;
	}

	.step-details {
		margin-left: 1rem;
	}

	.step-property {
		display: flex;
		margin-bottom: 0.5rem;
		font-size: 0.85rem;
	}

	.property-label {
		font-weight: 500;
		color: #6c757d;
		margin-right: 0.5rem;
		min-width: 70px;
	}

	.property-value {
		color: #2c3e50;
		font-family: monospace;
		word-break: break-all;
	}

	/* Workflow Actions */
	.workflow-actions {
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
		.workflows-container {
			grid-template-columns: 1fr;
		}

		.workflow-details {
			position: static;
		}
	}

	@media (max-width: 640px) {
		.workflow-stats {
			flex-direction: column;
			align-items: flex-start;
			gap: 0.5rem;
		}

		.workflow-actions {
			flex-direction: column;
		}

		.workflow-actions button {
			width: 100%;
		}
	}
</style>
