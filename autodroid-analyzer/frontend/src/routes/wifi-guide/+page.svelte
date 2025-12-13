<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  
  let wifiGuide: any = null;
  let loading = true;
  let error: string | null = null;
  
  onMount(async () => {
    try {
      const deviceId = $page.url.searchParams.get('device_id');
      const androidVersion = $page.url.searchParams.get('android_version');
      const connectionType = $page.url.searchParams.get('connection_type');
      
      if (!deviceId) {
        throw new Error('缺少设备ID参数');
      }
      
      // 获取WiFi调试指导数据
      const response = await fetch(`/api/devices/${deviceId}/wifi-debug-guide`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      wifiGuide = await response.json();
    } catch (err: any) {
      error = err.message;
      console.error('获取WiFi调试指导失败:', err);
    } finally {
      loading = false;
    }
  });
</script>

<svelte:head>
  <title>WiFi调试指导 - Autodroid Analyzer</title>
</svelte:head>

<div class="min-h-screen bg-gray-50 py-8">
  <div class="max-w-4xl mx-auto px-4">
    <!-- 页面标题 -->
    <div class="text-center mb-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">WiFi调试开通指导</h1>
      <p class="text-gray-600">简单几步，轻松开启WiFi调试功能</p>
    </div>

    {#if loading}
      <div class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
        <p class="text-gray-600">正在加载指导信息...</p>
      </div>
    {:else if error}
      <div class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <h3 class="text-red-800 font-medium mb-2">加载失败</h3>
        <p class="text-red-700">{error}</p>
      </div>
    {:else if wifiGuide}
      <!-- 设备信息卡片 -->
      <div class="bg-white rounded-lg shadow-md p-6 mb-6">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-semibold text-gray-900">Android {wifiGuide.android_version}</h2>
            <p class="text-gray-600">{wifiGuide.connection_type}连接方式</p>
          </div>
          <div class="text-right">
            <span class={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
              wifiGuide.supported 
                ? 'bg-green-100 text-green-800' 
                : 'bg-red-100 text-red-800'
            }`}>
              {wifiGuide.supported ? '✅ 支持WiFi调试' : '❌ 不支持'}
            </span>
            <p class="text-sm text-gray-500 mt-1">预计时间: {wifiGuide.estimated_time}</p>
          </div>
        </div>
      </div>

      <!-- 前置条件 -->
      {#if wifiGuide.requirements && wifiGuide.requirements.length > 0}
        <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-6 mb-6">
          <h3 class="text-lg font-semibold text-yellow-900 mb-3">📋 前置条件</h3>
          <ul class="space-y-2">
            {#each wifiGuide.requirements as requirement}
              <li class="flex items-start">
                <span class="text-yellow-600 mr-2">•</span>
                <span class="text-yellow-800">{requirement}</span>
              </li>
            {/each}
          </ul>
        </div>
      {/if}

      <!-- 操作步骤 -->
      <div class="bg-white rounded-lg shadow-md p-6 mb-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4">📝 操作步骤</h3>
        <div class="space-y-4">
          {#each wifiGuide.steps as step, index}
            <div class="flex items-start space-x-4">
              <div class="flex-shrink-0">
                <span class="w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center font-semibold">
                  {index + 1}
                </span>
              </div>
              <div class="flex-1">
                <h4 class="font-medium text-gray-900 mb-2">{step.title}</h4>
                <p class="text-gray-600">{step.description}</p>
                
                <!-- 图片占位符 - 可以添加实际截图 -->
                <div class="mt-3 bg-gray-100 border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
                  <div class="text-gray-400 mb-2">
                    <svg class="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                  </div>
                  <p class="text-gray-500 text-sm">步骤 {index + 1} 截图</p>
                  <p class="text-gray-400 text-xs">可添加实际设备操作截图</p>
                </div>
              </div>
            </div>
          {/each}
        </div>
      </div>

      <!-- ADB命令 -->
      {#if wifiGuide.commands && wifiGuide.commands.length > 0}
        <div class="bg-white rounded-lg shadow-md p-6 mb-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">💻 ADB命令</h3>
          <div class="space-y-3">
            {#each wifiGuide.commands as command}
              <div class="bg-gray-900 text-gray-100 rounded-lg p-4 font-mono text-sm">
                <code>{command}</code>
              </div>
            {/each}
          </div>
          <div class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p class="text-blue-800 text-sm">
              💡 <strong>提示：</strong> 复制命令后在命令行中执行，记得将<code class="bg-blue-100 px-1 rounded">&lt;IP地址&gt;</code>替换为实际设备IP
            </p>
          </div>
        </div>
      {/if}

      <!-- 底部操作按钮 -->
      <div class="text-center">
        <button 
          onclick={() => window.close()} 
          class="bg-gray-500 hover:bg-gray-600 text-white px-6 py-3 rounded-lg font-medium transition-colors"
        >
          关闭页面
        </button>
      </div>
    {/if}
  </div>
</div>