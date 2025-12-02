<script lang="ts">
	import type { UserProfile, UserPreferences } from '$lib/types';
	import { onMount } from 'svelte';

	let userProfile: UserProfile | null = null;
	let userPreferences: UserPreferences = {
		theme: 'light',
		language: 'en',
		notifications: true,
		email_alerts: false,
		auto_refresh: true,
		refresh_interval: 30
	};
	let isLoading = true;
	let error: string | null = null;
	let isEditing = false;

	async function fetchUserProfile() {
		isLoading = true;
		error = null;
		
		try {
			// For now, use mock data since the actual API might not exist yet
			// In a real implementation, this would be:
			// const profileResponse = await fetch('/api/my/profile');
			// const prefsResponse = await fetch('/api/my/preferences');
			// userProfile = await profileResponse.json();
			// userPreferences = await prefsResponse.json();
			
			// Mock data for demonstration
			userProfile = {
				id: 'user-1',
				username: 'admin',
				email: 'admin@example.com',
				name: 'Administrator',
				role: 'admin',
				last_login: '2025-01-01T10:00:00Z',
				created_at: '2024-01-01T00:00:00Z'
			};
			
			userPreferences = {
				theme: 'light',
				language: 'en',
				notifications: true,
				email_alerts: false,
				auto_refresh: true,
				refresh_interval: 30
			};
		} catch (err) {
			error = err instanceof Error ? err.message : 'An error occurred';
			console.error('Error fetching user profile:', err);
		} finally {
			isLoading = false;
		}
	}

	function toggleEdit() {
		isEditing = !isEditing;
	}

	async function savePreferences() {
		// In a real implementation, this would save to the API
		// await fetch('/api/my/preferences', {
		//     method: 'PUT',
		//     headers: {
		//         'Content-Type': 'application/json'
		//     },
		//     body: JSON.stringify(userPreferences)
		// });
		
		isEditing = false;
		// Show success message (in a real app, this would be a toast notification)
		console.log('Preferences saved successfully');
	}

	// Fetch user profile on component mount
	fetchUserProfile();
</script>

<div class="my-page">
	<h1>My Profile</h1>
	
	{#if isLoading}
		<div class="loading">Loading user profile...</div>
	{:else if error}
		<div class="error">Error: {error}</div>
	{:else}
		<div class="profile-container">
			<!-- User Profile Section -->
			<section class="profile-section">
				<h2>Profile Information</h2>
				<div class="profile-info">
					<div class="info-item">
						<span class="label">Name:</span>
						<span class="value">{userProfile?.name}</span>
					</div>
					<div class="info-item">
						<span class="label">Username:</span>
						<span class="value">{userProfile?.username}</span>
					</div>
					<div class="info-item">
						<span class="label">Email:</span>
						<span class="value">{userProfile?.email}</span>
					</div>
					<div class="info-item">
						<span class="label">Role:</span>
						<span class="value">{userProfile?.role}</span>
					</div>
					<div class="info-item">
						<span class="label">Last Login:</span>
						<span class="value">{new Date(userProfile?.last_login || '').toLocaleString()}</span>
					</div>
					<div class="info-item">
						<span class="label">Joined:</span>
						<span class="value">{new Date(userProfile?.created_at || '').toLocaleString()}</span>
					</div>
				</div>
			</section>
			
			<!-- User Preferences Section -->
			<section class="preferences-section">
				<div class="section-header">
					<h2>Preferences</h2>
					<button class="edit-button" on:click={toggleEdit}>
						{isEditing ? 'Cancel' : 'Edit'}
					</button>
				</div>
				
				<div class="preferences-form">
					<div class="form-group">
						<label for="theme">Theme:</label>
						<select 
							id="theme" 
							bind:value={userPreferences.theme}
							disabled={!isEditing}
						>
							<option value="light">Light</option>
							<option value="dark">Dark</option>
							<option value="system">System</option>
						</select>
					</div>
					
					<div class="form-group">
						<label for="language">Language:</label>
						<select 
							id="language" 
							bind:value={userPreferences.language}
							disabled={!isEditing}
						>
							<option value="en">English</option>
							<option value="zh">中文</option>
							<option value="ja">日本語</option>
						</select>
					</div>
					
					<div class="form-group checkbox-group">
						<input 
							type="checkbox" 
							id="notifications" 
							bind:checked={userPreferences.notifications}
							disabled={!isEditing}
						/>
						<label for="notifications">Enable Notifications</label>
					</div>
					
					<div class="form-group checkbox-group">
						<input 
							type="checkbox" 
							id="email_alerts" 
							bind:checked={userPreferences.email_alerts}
							disabled={!isEditing}
						/>
						<label for="email_alerts">Enable Email Alerts</label>
					</div>
					
					<div class="form-group checkbox-group">
						<input 
							type="checkbox" 
							id="auto_refresh" 
							bind:checked={userPreferences.auto_refresh}
							disabled={!isEditing}
						/>
						<label for="auto_refresh">Auto Refresh Dashboard</label>
					</div>
					
					<div class="form-group">
						<label for="refresh_interval">Refresh Interval (seconds):</label>
						<input 
							type="number" 
							id="refresh_interval" 
							bind:value={userPreferences.refresh_interval}
							disabled={!isEditing || !userPreferences.auto_refresh}
							min="5"
							max="300"
							step="5"
						/>
					</div>
					
					{#if isEditing}
						<div class="form-actions">
							<button class="primary-button" on:click={savePreferences}>Save Changes</button>
							<button class="secondary-button" on:click={toggleEdit}>Cancel</button>
						</div>
					{/if}
				</div>
			</section>
			
			<!-- Recent Activity Section -->
			<section class="activity-section">
				<h2>Recent Activity</h2>
				<div class="activity-list">
					<div class="activity-item">
						<div class="activity-time">{new Date().toLocaleString()}</div>
						<div class="activity-description">Logged in to the system</div>
					</div>
					<div class="activity-item">
						<div class="activity-time">{new Date(Date.now() - 3600000).toLocaleString()}</div>
						<div class="activity-description">Updated workflow "Test Workflow"</div>
					</div>
					<div class="activity-item">
						<div class="activity-time">{new Date(Date.now() - 7200000).toLocaleString()}</div>
						<div class="activity-description">Created new test plan</div>
					</div>
					<div class="activity-item">
						<div class="activity-time">{new Date(Date.now() - 10800000).toLocaleString()}</div>
						<div class="activity-description">Viewed report "Test Report 1"</div>
					</div>
				</div>
			</section>
		</div>
	{/if}
</div>

<style>
	.my-page {
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

	/* Profile Container */
	.profile-container {
		display: grid;
		grid-template-columns: 1fr 350px;
		gap: 2rem;
	}

	/* Profile Section */
	.profile-section {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
	}

	.profile-section h2 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 1.5rem;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
	}

	.profile-info {
		display: grid;
		grid-template-columns: 1fr;
		gap: 1rem;
	}

	.info-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0.75rem 1rem;
		background-color: #f8f9fa;
		border-radius: 6px;
	}

	.info-item .label {
		font-weight: 500;
		color: #6c757d;
	}

	.info-item .value {
		font-weight: 600;
		color: #2c3e50;
	}

	/* Preferences Section */
	.preferences-section {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		grid-column: 1;
	}

	.section-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 1.5rem;
	}

	.section-header h2 {
		color: #2c3e50;
		margin: 0;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
		flex: 1;
	}

	.edit-button {
		background-color: #3498db;
		color: white;
		border: none;
		padding: 0.5rem 1rem;
		border-radius: 6px;
		cursor: pointer;
		font-weight: 500;
		transition: background-color 0.2s ease;
	}

	.edit-button:hover {
		background-color: #2980b9;
	}

	/* Preferences Form */
	.preferences-form {
		display: grid;
		grid-template-columns: 1fr;
		gap: 1.5rem;
	}

	.form-group {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.form-group label {
		font-weight: 500;
		color: #2c3e50;
		font-size: 0.9rem;
	}

	.form-group select,
	.form-group input[type="number"] {
		padding: 0.75rem;
		border: 1px solid #ddd;
		border-radius: 6px;
		font-size: 0.9rem;
		transition: border-color 0.2s ease;
	}

	.form-group select:focus,
	.form-group input[type="number"]:focus {
		outline: none;
		border-color: #3498db;
	}

	.form-group input:disabled,
	.form-group select:disabled {
		background-color: #f0f0f0;
		cursor: not-allowed;
	}

	.checkbox-group {
		flex-direction: row;
		align-items: center;
		gap: 0.75rem;
	}

	.checkbox-group input[type="checkbox"] {
		width: 16px;
		height: 16px;
	}

	.checkbox-group label {
		margin: 0;
		cursor: pointer;
	}

	.form-actions {
		display: flex;
		gap: 1rem;
		justify-content: flex-end;
		margin-top: 1rem;
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

	/* Activity Section */
	.activity-section {
		background: white;
		border-radius: 8px;
		padding: 1.5rem;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		grid-column: 2;
		grid-row: 1 / span 2;
		height: fit-content;
		position: sticky;
		top: 2rem;
	}

	.activity-section h2 {
		color: #2c3e50;
		margin-top: 0;
		margin-bottom: 1.5rem;
		font-size: 1.25rem;
		border-bottom: 2px solid #ecf0f1;
		padding-bottom: 0.5rem;
	}

	.activity-list {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.activity-item {
		padding: 1rem;
		background-color: #f8f9fa;
		border-radius: 6px;
		border-left: 4px solid #3498db;
	}

	.activity-time {
		font-size: 0.8rem;
		color: #6c757d;
		margin-bottom: 0.25rem;
	}

	.activity-description {
		font-size: 0.9rem;
		color: #2c3e50;
	}

	/* Responsive Design */
	@media (max-width: 1024px) {
		.profile-container {
			grid-template-columns: 1fr;
		}
		
		.activity-section {
			grid-column: 1;
			grid-row: auto;
			position: static;
		}
	}

	@media (max-width: 640px) {
		.section-header {
			flex-direction: column;
			align-items: flex-start;
			gap: 1rem;
		}
		
		.form-actions {
			flex-direction: column;
		}
		
		.form-actions button {
			width: 100%;
		}
	}
</style>