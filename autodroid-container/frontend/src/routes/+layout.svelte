<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	
	let favicon = '/favicon.ico';

	import { _requiresAuth } from './+layout.ts';

	let { children } = $props();

	let isLoading = $state(true);

	$effect(() => {
		// 检查当前路由是否需要认证
		const currentPath = window.location.pathname;
		
		if (_requiresAuth(currentPath) && !$page.data?.authState?.isAuthenticated) {
			goto('/auth/login');
			return;
		}
		
		// 如果已登录但访问认证页面，重定向到首页
		if ($page.data?.authState?.isAuthenticated && currentPath.startsWith('/auth')) {
			goto('/');
			return;
		}
		
		isLoading = false;
	});

	async function logout() {
		try {
			const token = localStorage.getItem('auth_token');
			if (token) {
				await fetch('http://localhost:8003/api/auth/logout', {
					method: 'POST',
					headers: {
						'Authorization': `Bearer ${token}`
					}
				});
			}
			
			// 清除本地存储
			localStorage.removeItem('auth_token');
			localStorage.removeItem('user_data');
			// 使用强制刷新跳转到登录页
			window.location.href = '/auth/login';
		} catch (error) {
			console.error('Logout error:', error);
			// 即使API调用失败也要清除本地存储并重定向
			localStorage.removeItem('auth_token');
			localStorage.removeItem('user_data');
			window.location.href = '/auth/login';
		}
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{#if !isLoading}
		{#if window.location.pathname.startsWith('/auth')}
			<!-- 认证页面不显示导航栏 -->
		{:else}
			<nav class="navbar">
				<div class="navbar-brand">
					<h1>Autodroid Admin</h1>
				</div>
				<div class="navbar-menu">
					<a href="/" class:active={window.location.pathname === '/'}>Dashboard</a>
					<a href="/workflows" class:active={window.location.pathname.startsWith('/workflows')}>Workflows</a>
					<a href="/reports" class:active={window.location.pathname.startsWith('/reports')}>Reports</a>
					<a href="/orders" class:active={window.location.pathname.startsWith('/orders')}>Orders</a>
					<a href="/my" class:active={window.location.pathname.startsWith('/my')}>My</a>
				</div>
			{#if $page.data?.authState?.isAuthenticated && $page.data?.authState?.user}
			<div class="navbar-user">
				<span class="user-info">欢迎，{$page.data.authState.user.full_name || $page.data.authState.user.email}</span>
				<button onclick={logout} class="logout-button">退出</button>
			</div>
		{/if}
		</nav>
	{/if}
{/if}

<main class="main-content">
	{@render children()}
</main>

<style>
	.navbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 1rem 2rem;
		background: #2c3e50;
		color: white;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
	}

	.navbar-brand h1 {
		margin: 0;
		font-size: 1.5rem;
		font-weight: 600;
	}

	.navbar-menu {
		display: flex;
		gap: 2rem;
	}

	.navbar-menu a {
		color: white;
		text-decoration: none;
		font-weight: 500;
		padding: 0.5rem 1rem;
		border-radius: 4px;
		transition: background-color 0.2s ease;
	}

	.navbar-menu a:hover, .navbar-menu a.active {
		background-color: #34495e;
	}

	.main-content {
		padding: 2rem;
		max-width: 1200px;
		margin: 0 auto;
	}

	.navbar-user {
		display: flex;
		align-items: center;
		gap: 1rem;
	}

	.user-info {
		color: white;
		font-size: 0.9rem;
	}

	.logout-button {
		background: #e74c3c;
		color: white;
		border: none;
		padding: 0.5rem 1rem;
		border-radius: 4px;
		font-size: 0.8rem;
		cursor: pointer;
		transition: background-color 0.2s ease;
	}

	.logout-button:hover {
		background: #c0392b;
	}
</style>
