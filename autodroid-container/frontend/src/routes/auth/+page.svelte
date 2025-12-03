<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { API_CONFIG } from '$lib/config';

	let email = '15317227@qq.com'; // 默认邮箱
	let password = '123456'; // 默认密码
	let confirmPassword = '123456'; // 确认密码默认值
	let error = '';
	let isLoading = false;
	let showPassword = false;
	let showConfirmPassword = false;

	async function register() {
		if (password !== confirmPassword) {
			error = '密码不匹配';
			return;
		}

		if (password.length < 6) {
			error = '密码长度至少6位';
			return;
		}

		isLoading = true;
		error = '';

		try {
			const response = await fetch(API_CONFIG.REGISTER_URL, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({
					email: email,
					password: password
				}),
			});

			if (response.ok) {
				const data = await response.json();
				// 存储用户数据
				localStorage.setItem('user_data', JSON.stringify(data));
				// 注册成功后自动登录
				await login();
			} else {
			const errorData = await response.json();
			error = typeof errorData.detail === 'string' ? errorData.detail : JSON.stringify(errorData.detail) || '注册失败';
		}
		} catch (err) {
			error = '网络连接错误';
		} finally {
			isLoading = false;
		}
	}

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
				// 跳转到首页
				goto('/app/');
			} else {
			const errorData = await response.json();
			error = typeof errorData.detail === 'string' ? errorData.detail : JSON.stringify(errorData.detail) || '登录失败';
		}
		} catch (err) {
			error = '网络连接错误';
		} finally {
			isLoading = false;
		}
	}

	function goToLogin() {
		goto('/app/auth/login');
	}
</script>

<div class="auth-container">
	<div class="auth-card">
		<h1>注册账户</h1>
		<p class="auth-subtitle">创建您的Autodroid账户</p>

		{#if error}
			<div class="error-message">{error}</div>
		{/if}

		<form on:submit|preventDefault={register} class="auth-form">
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

			<div class="form-group">
				<label for="confirmPassword">确认密码</label>
				<div class="relative">
					<input
						id="confirmPassword"
						type={showConfirmPassword ? 'text' : 'password'}
						bind:value={confirmPassword}
						placeholder="请再次输入密码"
						class="w-full pr-10"
						required
					/>
					<button type="button" class="absolute right-2 top-1/2 transform -translate-y-1/2 p-1 rounded hover:bg-gray-100 transition-colors" on:click={() => showConfirmPassword = !showConfirmPassword}>
						{#if showConfirmPassword}
							👁️
						{:else}
							👁️‍🗨️
						{/if}
					</button>
				</div>
			</div>

			<button type="submit" disabled={isLoading} class="auth-button">
				{#if isLoading}
					<span>注册中...</span>
				{:else}
					<span>注册</span>
				{/if}
			</button>
		</form>

		<div class="auth-links">
			<span>已有账户？</span>
			<button on:click={goToLogin} class="link-button">立即登录</button>
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