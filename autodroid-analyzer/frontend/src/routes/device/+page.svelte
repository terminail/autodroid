<script lang="ts">
  import type { DeviceInfo } from "$lib/types";
  import { onMount } from "svelte";

  let devices: DeviceInfo[] = [];
  let loading: boolean = true;
  let error: string | null = null;
  // 移除了复杂的模态框变量，改用简单的HTML页面

  // 获取设备列表
  async function fetchDevices() {
    try {
      loading = true;
      error = null;

      const response = await fetch("/api/devices/");
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      devices = await response.json();
    } catch (err: any) {
      error = err.message;
      console.error("获取设备列表失败:", err);
    } finally {
      loading = false;
    }
  }

  // 获取WiFi调试指导 - 现在直接打开HTML页面
  async function getWifiDebugGuide(device: DeviceInfo): Promise<void> {
    try {
      // 直接在新标签页中打开WiFi调试指导页面
      window.open(
        `/wifi-guide?device_id=${device.id}&android_version=${device.android_version}&connection_type=${device.connection_type}`,
        "_blank",
      );
    } catch (err: any) {
      console.error("打开WiFi调试指导失败:", err);
      alert("打开WiFi调试指导失败: " + err.message);
    }
  }

  // 刷新设备列表
  function refreshDevices() {
    fetchDevices();
  }

  // 格式化时间
  function formatTime(dateString: string | null): string {
    if (!dateString) return "从未连接";
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    if (diff < 60000) return "刚刚";
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
    return `${Math.floor(diff / 86400000)}天前`;
  }

  // 获取连接状态颜色
  function getConnectionColor(device: DeviceInfo): string {
    if (device.is_connected) {
      return device.connection_type === "WiFi"
        ? "text-green-600"
        : "text-blue-600";
    }
    return "text-gray-500";
  }

  // 获取电池图标
  function getBatteryIcon(level: number): string {
    if (level >= 80) return "🔋";
    if (level >= 50) return "🔋";
    if (level >= 20) return "🪫";
    return "🪫";
  }

  // 复制功能已移除，使用简单的HTML页面代替

  onMount(() => {
    fetchDevices();

    // 每30秒自动刷新
    const interval = setInterval(fetchDevices, 30000);

    return () => {
      clearInterval(interval);
    };
  });
</script>

<svelte:head>
  <title>设备管理 - Autodroid Analyzer</title>
</svelte:head>

<div class="container mx-auto px-4 py-8">
  <!-- 页面标题 -->
  <div class="flex justify-between items-center mb-8">
    <h1 class="text-3xl font-bold text-gray-800">设备管理</h1>
    <button
      on:click={refreshDevices}
      class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center transition-colors"
    >
      <svg
        class="w-4 h-4 mr-2"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
        ></path>
      </svg>
      刷新
    </button>
  </div>

  <!-- 错误提示 -->
  {#if error}
    <div
      class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4"
    >
      <strong>错误：</strong>
      {error}
    </div>
  {/if}

  <!-- 加载状态 -->
  {#if loading}
    <div class="flex justify-center items-center py-12">
      <div
        class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"
      ></div>
      <span class="ml-3 text-gray-600">正在加载设备列表...</span>
    </div>
  {:else if devices.length === 0}
    <!-- 空状态 -->
    <div class="text-center py-12">
      <svg
        class="w-16 h-16 text-gray-400 mx-auto mb-4"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"
        ></path>
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">暂无设备</h3>
      <p class="text-gray-500">请连接Android设备并确保已启用USB调试</p>
    </div>
  {:else}
    <!-- 设备卡片列表 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {#each devices as device}
        <div
          class="bg-white rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 overflow-hidden"
        >
          <!-- 卡片头部 -->
          <div class="bg-gradient-to-r from-blue-500 to-purple-600 px-6 py-4">
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <h3
                  class="text-white font-semibold text-lg truncate"
                  title={device.device_name}
                >
                  {device.device_name || device.id}
                </h3>
                <p class="text-blue-100 text-sm">
                  ID: {device.id.substring(0, 8)}...
                </p>
              </div>
              <div class="flex items-center">
                <span
                  class={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    device.is_connected
                      ? "bg-green-100 text-green-800"
                      : "bg-red-100 text-red-800"
                  }`}
                >
                  {device.is_connected ? "已连接" : "未连接"}
                </span>
              </div>
            </div>
          </div>

          <!-- 卡片内容 -->
          <div class="px-6 py-4 space-y-3">
            <!-- 系统信息 -->
            <div class="flex items-center text-sm text-gray-600">
              <svg
                class="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"
                ></path>
              </svg>
              Android {device.android_version} (API {device.api_level})
            </div>

            <!-- 设备型号 -->
            <div class="flex items-center text-sm text-gray-600">
              <svg
                class="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"
                ></path>
              </svg>
              {device.device_model}
            </div>

            <!-- 连接信息 -->
            <div class="flex items-center text-sm {getConnectionColor(device)}">
              <svg
                class="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                {#if device.connection_type === "WiFi"}
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0"
                  ></path>
                {:else}
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                  ></path>
                {/if}
              </svg>
              {device.connection_type} 连接
            </div>

            <!-- 电池电量 -->
            {#if device.battery_level > 0}
              <div class="flex items-center text-sm text-gray-600">
                <span class="mr-2">{getBatteryIcon(device.battery_level)}</span>
                电量: {device.battery_level}%
              </div>
            {/if}

            <!-- 最后连接时间 -->
            <div class="flex items-center text-sm text-gray-500">
              <svg
                class="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                ></path>
              </svg>
              最后连接: {formatTime(device.last_connected)}
            </div>
          </div>

          <!-- 卡片底部操作按钮 -->
          <div class="px-6 py-4 bg-gray-50 border-t border-gray-200">
            <div class="flex space-x-2">
              {#if device.is_connected && device.connection_type === "USB"}
                <button
                  on:click={() => getWifiDebugGuide(device)}
                  class="flex-1 bg-green-500 hover:bg-green-600 text-white px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                >
                  开通WiFi调试
                </button>
              {/if}
              <a
                href="/device/{device.id}"
                class="flex-1 bg-blue-500 hover:bg-blue-600 text-white px-3 py-2 rounded-lg text-sm font-medium text-center transition-colors"
              >
                查看详情
              </a>
            </div>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>

<!-- WiFi调试指导现在通过简单的HTML页面打开 -->
