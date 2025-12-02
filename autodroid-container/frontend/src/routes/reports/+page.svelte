<script lang="ts">
	import type { TestReport, TestReportStep } from '$lib/types';
	import { onMount } from 'svelte';

	let reports: TestReport[] = [];
	let isLoading = true;
	let error: string | null = null;
	let selectedReport: TestReport | null = null;
	let statusFilter: string = 'all';
	let searchQuery: string = '';

	async function fetchReports() {
		isLoading = true;
		error = null;
		
		try {
			// For now, use a mock endpoint since the actual API might not exist yet
			// In a real implementation, this would be:
			// const response = await fetch('/api/reports');
			// const data = await response.json();
			// reports = data.reports || [];
			
			// Mock data for demonstration
			reports = [
				{
					id: 'report-1',
					name: 'Test Report 1',
					description: 'Test report for workflow 1 on device 1',
					workflow_name: 'workflow-1',
					device_udid: 'device-123',
					status: 'passed',
					start_time: '2025-01-01T10:00:00Z',
					end_time: '2025-01-01T10:05:30Z',
					duration: 330,
					steps: [
						{
							id: 'step-1',
							action: 'click',
							status: 'passed',
							duration: 1000,
							selector: '#button1'
						},
						{
							id: 'step-2',
							action: 'fill',
							status: 'passed',
							duration: 500,
							selector: '#input1',
							value: 'test value'
						},
						{
							id: 'step-3',
							action: 'submit',
							status: 'passed',
							duration: 2000,
							selector: '#form1'
						}
					]
				},
				{
					id: 'report-2',
					name: 'Test Report 2',
					description: 'Test report for workflow 2 on device 2',
					workflow_name: 'workflow-2',
					device_udid: 'device-456',
					status: 'failed',
					start_time: '2025-01-02T14:30:00Z',
					end_time: '2025-01-02T14:32:15Z',
					duration: 135,
					steps: [
						{
							id: 'step-1',
							action: 'click',
							status: 'passed',
							duration: 800,
							selector: '#button2'
						},
						{
							id: 'step-2',
							action: 'fill',
							status: 'failed',
							duration: 300,
							selector: '#input2',
							value: 'test value',
							error: 'Element not found'
						}
					]
				},
				{
					id: 'report-3',
					name: 'Test Report 3',
					description: 'Test report for workflow 1 on device 3',
					workflow_name: 'workflow-1',
					device_udid: 'device-789',
					status: 'passed',
					start_time: '2025-01-03T09:15:00Z',
					end_time: '2025-01-03T09:20:45Z',
					duration: 345,
					steps: [
						{
							id: 'step-1',
							action: 'click',
							status: 'passed',
							duration: 1200,
							selector: '#button1'
						},
						{
							id: 'step-2',
							action: 'fill',
							status: 'passed',
							duration: 600,
							selector: '#input1',
							value: 'test value'
						},
						{
							id: 'step-3',
							action: 'submit',
							status: 'passed',
							duration: 2500,
							selector: '#form1'
						}
					]
				}
			];
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching reports:', err);
		} finally {
			isLoading = false;
		}
	}

	function getFilteredReports() {
		return reports.filter(report => {
			// Status filter
			const matchesStatus = statusFilter === 'all' || report.status === statusFilter;
			
			// Search query filter
			const matchesSearch = !searchQuery || 
				report.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
				report.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
				report.workflow_name.toLowerCase().includes(searchQuery.toLowerCase());
			
			return matchesStatus && matchesSearch;
		});
	}

	function selectReport(report: TestReport) {
		selectedReport = report;
	}

	function closeReportDetails() {
		selectedReport = null;
	}

	// Fetch reports on component mount
	onMount(fetchReports);
</script>

<div class="reports-page">
	<h1>Reports</h1>
	
	{#if isLoading}
		<div class="loading">Loading reports...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="reports-container">
			<!-- Reports List -->
			<div class="reports-list">
				<div class="reports-header">
					<h2>Report List</h2>
					<div class="filters">
						<div class="search-box">
							<input 
								type="text" 
								placeholder="Search reports..." 
								bind:value={searchQuery}
								class="search-input"
							/>
						</div>
						<div class="status-filter">
							<select bind:value={statusFilter} class="filter-select">
								<option value="all">All Status</option>
								<option value="passed">Passed</option>
								<option value="failed">Failed</option>
								<option value="running">Running</option>
								<option value="pending">Pending</option>
							</select>
						</div>
					</div>
				</div>
				
				{#if getFilteredReports().length === 0}
					<div class="no-reports">No reports found</div>
				{:else}
					<div class="report-cards">
						{#each getFilteredReports() as report}
							<div class="report-card" on:click={() => selectReport(report)} on:keydown={(e) => { if (e.key === 'Enter' || e.key === ' ') selectReport(report); }} role="button" tabindex="0">
								<div class="report-header">
									<h3>{report.name}</h3>
									<span class={`status-badge ${report.status}`}>
										{report.status.charAt(0).toUpperCase() + report.status.slice(1)}
									</span>
								</div>
								<p class="description">{report.description || 'No description'}</p>
								<div class="report-meta">
									<div class="meta-item">
										<span class="label">Workflow:</span>
										<span class="value">{report.workflow_name}</span>
									</div>
									<div class="meta-item">
										<span class="label">Device:</span>
										<span class="value">{report.device_udid}</span>
									</div>
									<div class="meta-item">
										<span class="label">Duration:</span>
										<span class="value">{report.duration}s</span>
									</div>
									<div class="meta-item">
										<span class="label">Start:</span>
										<span class="value">{new Date(report.start_time).toLocaleString()}</span>
									</div>
								</div>
							</div>
						{/each}
					</div>
				{/if}
			</div>
			
			<!-- Report Details -->
			{#if selectedReport}
				<div class="report-details">
					<div class="details-header">
						<h2>Report Details</h2>
						<button class="close-button" on:click={closeReportDetails}>×</button>
					</div>
					
					<div class="details-content">
						<div class="report-info">
							<h3>{selectedReport.name}</h3>
							<p class="description">{selectedReport.description || 'No description'}</p>
							<div class="report-status">
								<span class={`status-badge ${selectedReport.status}`}>
									{selectedReport.status.charAt(0).toUpperCase() + selectedReport.status.slice(1)}
								</span>
								<span class="duration">Duration: {selectedReport.duration}s</span>
							</div>
						</div>
						
						<div class="report-metadata">
							<h4>Metadata</h4>
							<div class="metadata-grid">
								<div class="metadata-item">
									<span class="meta-key">Workflow:</span>
									<span class="meta-value">{selectedReport.workflow_name}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Device UDID:</span>
									<span class="meta-value">{selectedReport.device_udid}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Start Time:</span>
									<span class="meta-value">{new Date(selectedReport.start_time).toLocaleString()}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">End Time:</span>
									<span class="meta-value">{new Date(selectedReport.end_time).toLocaleString()}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Total Steps:</span>
									<span class="meta-value">{selectedReport.steps.length}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Passed Steps:</span>
									<span class="meta-value">{selectedReport.steps.filter(step => step.status === 'passed').length}</span>
								</div>
								<div class="metadata-item">
									<span class="meta-key">Failed Steps:</span>
									<span class="meta-value">{selectedReport.steps.filter(step => step.status === 'failed').length}</span>
								</div>
							</div>
						</div>
						
						<div class="report-steps">
							<h4>Steps</h4>
							<div class="steps-list">
								{#each selectedReport.steps as step, index}
									<div class="step-item" class:failed={step.status === 'failed'}>
										<div class="step-header">
											<span class="step-number">{index + 1}</span>
											<span class="step-action">{step.action}</span>
											<span class={`step-status ${step.status}`}>
												{step.status.charAt(0).toUpperCase() + step.status.slice(1)}
											</span>
											<span class="step-duration">{step.duration}ms</span>
										</div>
										{#if step.error}
											<div class="step-error">
												<strong>Error:</strong> {step.error}
											</div>
										{/if}
										{#if step.selector}
											<div class="step-property">
												<span class="property-label">Selector:</span>
												<span class="property-value">{step.selector}</span>
											</div>
										{/if}
										{#if step.value !== undefined}
											<div class="step-property">
												<span class="property-label">Value:</span>
												<span class="property-value">{JSON.stringify(step.value)}</span>
											</div>
										{/if}
									</div>
								{/each}
							</div>
						</div>
						
						<div class="report-actions">
							<button class="primary-button">Download Report</button>
							<button class="secondary-button">Delete Report</button>
							<button class="secondary-button">Rerun Workflow</button>
						</div>
					</div>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.reports-page {
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

	/* Reports Container */
	.reports-container {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 2rem;
	}

	/* Reports List */
	.reports-list {
		grid-column: 1;
	}

	.reports-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 1.5rem;
	}

	.reports-header h2 {
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

	.no-reports {
		text-align: center;
		padding: 2rem;
		background-color: #f8f9fa;
		border-radius: 8px;
		color: #6c757d;
	}

	.report-cards {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.report-card {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		cursor: pointer;
		transition: all 0.2s ease;
		border: 2px solid transparent;
	}

	.report-card:hover {
		transform: translateY(-2px);
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
		border-color: #3498db;
	}

	.report-header {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-bottom: 0.5rem;
	}

	.report-header h3 {
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

	.status-badge.passed {
		background-color: #e8f5e8;
		color: #27ae60;
	}

	.status-badge.failed {
		background-color: #ffebee;
		color: #d32f2f;
	}

	.status-badge.running {
		background-color: #fff3e0;
		color: #f39c12;
	}

	.status-badge.pending {
		background-color: #e3f2fd;
		color: #1976d2;
	}

	.report-card .description {
		color: #6c757d;
		margin-bottom: 1rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.report-meta {
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

	/* Report Details */
	.report-details {
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

	/* Report Info */
	.report-info h3 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 0.5rem;
		font-size: 1.1rem;
	}

	.report-info .description {
		color: #6c757d;
		margin-bottom: 1rem;
		font-size: 0.9rem;
		line-height: 1.4;
	}

	.report-status {
		display: flex;
		align-items: center;
		gap: 1rem;
		margin-bottom: 1.5rem;
	}

	.report-status .duration {
		font-size: 0.9rem;
		color: #6c757d;
	}

	/* Report Metadata */
	.report-metadata h4,
	.report-steps h4 {
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

	/* Report Steps */
	.steps-list {
		max-height: 500px;
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

	.step-item.failed {
		background-color: #ffebee;
		border-left-color: #d32f2f;
	}

	.step-header {
		display: flex;
		gap: 0.75rem;
		align-items: center;
		margin-bottom: 0.75rem;
		flex-wrap: wrap;
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

	.step-item.failed .step-number {
		background-color: #d32f2f;
	}

	.step-action {
		font-weight: 600;
		color: #2c3e50;
		font-size: 0.9rem;
		flex: 1;
	}

	.step-status {
		padding: 0.25rem 0.5rem;
		border-radius: 4px;
		font-size: 0.8rem;
		font-weight: 500;
	}

	.step-status.passed {
		background-color: #e8f5e8;
		color: #27ae60;
	}

	.step-status.failed {
		background-color: #ffebee;
		color: #d32f2f;
	}

	.step-duration {
		font-size: 0.8rem;
		color: #6c757d;
	}

	.step-error {
		background-color: #ffebee;
		color: #d32f2f;
		padding: 0.75rem;
		border-radius: 4px;
		margin: 0.5rem 0;
		font-size: 0.85rem;
		border-left: 3px solid #d32f2f;
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

	/* Report Actions */
	.report-actions {
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
		.reports-container {
			grid-template-columns: 1fr;
		}
		
		.report-details {
			position: static;
		}
		
		.reports-header {
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
		
		.report-actions {
			flex-direction: column;
		}
		
		.report-actions button {
			width: 100%;
		}
	}
</style>