<script lang="ts">
	import { goto } from '$app/navigation';
	import { API_CONFIG } from '$lib/config';

	let email = '15317227@qq.com'; // 默认邮箱
	let password = '123456'; // 默认密码
	let error = '';
	let isLoading = false;
	let showPassword = false;

	async function login() {
		isLoading = true;
		error = '';

		try {
			const response = await fetch(API_CONFIG.LOGIN_URL, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({
					email: email,
					password: password,
				}),
			});

			if (response.ok) {
				const data = await response.json();
				// 存储token
				localStorage.setItem('auth_token', data.access_token);
				// 获取用户信息并存储
				try {
					const userResponse = await fetch(API_CONFIG.ME_URL, {
						headers: {
							'Authorization': `Bearer ${data.access_token}`
						}
					});
					if (userResponse.ok) {
						const userData = await userResponse.json();
						localStorage.setItem('user_data', JSON.stringify(userData));
					}
				} catch (error) {
					console.error('Failed to fetch user data:', error);
				}
				// 跳转到首页
				window.location.href = '/app';
			} else {
				const errorData = await response.json();
				error = errorData.detail || '登录失败';
				// 认证失败时不要跳转，停留在登录页面
				return;
			}
		} catch (err) {
			error = '网络连接错误';
		} finally {
			isLoading = false;
		}
	}

	function goToRegister() {
		goto('/app/auth');
	}
</script>

<div class="auth-container">
	<div class="auth-card">
		<h1>登录账户</h1>
		<p class="auth-subtitle">登录您的Autodroid账户</p>

		{#if error}
			<div class="error-message">{error}</div>
		{/if}

		<form on:submit|preventDefault={login} class="auth-form">
			<div class="form-group">
				<label for="email">电子邮箱</label>
				<input
					id="email"
					type="email"
					bind:value={email}
					placeholder="请输入您的邮箱"
					required
				/>
			</div>

			<div class="form-group">
				<label for="password">密码</label>
				<div class="relative">
					<input
						id="password"
						type={showPassword ? 'text' : 'password'}
						bind:value={password}
						placeholder="请输入密码"
						class="w-full pr-10"
						required
					/>
					<button type="button" class="absolute right-2 top-1/2 transform -translate-y-1/2 p-1 rounded hover:bg-gray-100 transition-colors" on:click={() => showPassword = !showPassword}>
						{#if showPassword}
							👁️
						{:else}
							👁️‍🗨️
						{/if}
					</button>
				</div>
			</div>

			<button type="submit" disabled={isLoading} class="auth-button">
				{#if isLoading}
					<span>登录中...</span>
				{:else}
					<span>登录</span>
				{/if}
			</button>
		</form>

		<div class="auth-links">
			<span>没有账户？</span>
			<button on:click={goToRegister} class="link-button">立即注册</button>
		</div>
	</div>
</div>

<style>
	.auth-container {
		display: flex;
		justify-content: center;
		align-items: center;
		min-height: 100vh;
		background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
		padding: 2rem;
	}

	.auth-card {
		background: white;
		padding: 3rem;
		border-radius: 12px;
		box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
		width: 100%;
		max-width: 400px;
	}

	h1 {
		text-align: center;
		margin-bottom: 0.5rem;
		color: #2c3e50;
		font-size: 2rem;
		font-weight: 600;
	}

	.auth-subtitle {
		text-align: center;
		color: #7f8c8d;
		margin-bottom: 2rem;
	}

	.auth-form {
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
	}

	.form-group {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	label {
		font-weight: 500;
		color: #2c3e50;
		font-size: 0.9rem;
	}

	input {
		padding: 0.75rem;
		border: 2px solid #e0e0e0;
		border-radius: 6px;
		font-size: 1rem;
		transition: border-color 0.2s ease;
	}

	input:focus {
		outline: none;
		border-color: #667eea;
	}

	.auth-button {
		background: #667eea;
		color: white;
		border: none;
		padding: 0.75rem;
		border-radius: 6px;
		font-size: 1rem;
		font-weight: 500;
		cursor: pointer;
		transition: background-color 0.2s ease;
	}

	.auth-button:hover:not(:disabled) {
		background: #5a6fd8;
	}

	.auth-button:disabled {
		background: #bdc3c7;
		cursor: not-allowed;
	}

	.error-message {
		background: #e74c3c;
		color: white;
		padding: 0.75rem;
		border-radius: 6px;
		margin-bottom: 1rem;
		text-align: center;
		font-size: 0.9rem;
	}

	.auth-links {
		text-align: center;
		margin-top: 1.5rem;
		color: #7f8c8d;
	}

	.link-button {
		background: none;
		border: none;
		color: #667eea;
		cursor: pointer;
		text-decoration: underline;
		font-size: 0.9rem;
	}

	.link-button:hover {
		color: #5a6fd8;
	}
</style>