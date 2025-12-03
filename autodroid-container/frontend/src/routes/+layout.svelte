<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { API_CONFIG } from '$lib/config';
	
	let favicon = '/favicon.ico';

	import { _requiresAuth } from './+layout.ts';

	let { children } = $props();

	let isLoading = $state(true);

	$effect(() => {
		// 检查当前路由是否需要认证
		const currentPath = window.location.pathname;
		
		if (_requiresAuth(currentPath) && !$page.data?.authState?.isAuthenticated) {
			goto('/app/auth/login');
			return;
		}
		
		// 如果已登录但访问认证页面，重定向到首页
		if ($page.data?.authState?.isAuthenticated && currentPath.startsWith('/app/auth')) {
			goto('/');
			return;
		}
		
		isLoading = false;
	});

	function logout(event: MouseEvent) {
		// 阻止任何默认行为和事件传播
		event.preventDefault();
		event.stopPropagation();
		
		// 清除本地存储
		localStorage.removeItem('auth_token');
		localStorage.removeItem('user_data');
		
		// 使用最直接的方式跳转，确保不会有任何API调用
		window.location.replace('/app/auth/login');
		return false;
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{#if !isLoading}
		{#if window.location.pathname.startsWith('/app/auth')}
			<!-- 认证页面不显示导航栏 -->
		{:else}
			<nav class="navbar">
				<div class="navbar-brand">
					<h1>Autodroid Admin</h1>
				</div>
				<div class="navbar-menu">
					<a href="/app" class:active={window.location.pathname === '/' || window.location.pathname === '/app'}>Dashboard</a>
					<a href="/app/workflows" class:active={window.location.pathname.startsWith('/app/workflows')}>Workflows</a>
					<a href="/app/reports" class:active={window.location.pathname.startsWith('/app/reports')}>Reports</a>
					<a href="/app/orders" class:active={window.location.pathname.startsWith('/app/orders')}>Orders</a>
					<a href="/app/my" class:active={window.location.pathname.startsWith('/app/my')}>My</a>
				</div>
			{#if $page.data?.authState?.isAuthenticated && $page.data?.authState?.user}
			<div class="navbar-user">
				<span class="user-info">欢迎，{$page.data.authState.user.full_name || $page.data.authState.user.email}</span>
				<button on:click={logout} type="button" class="logout-button">退出</button>
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
