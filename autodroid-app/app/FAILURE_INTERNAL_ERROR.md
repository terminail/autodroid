# Internal Error: mDNS Discovery Failed

## 从日志来看，你的mDNS服务发现遇到了问题。让我帮你分析和解决这个问题：

## 问题分析

错误信息显示：
```
Discovery failed: Error code:0, ServiceType:_autodroid._tcp.local.
Error meaning: FAILURE_INTERNAL_ERROR
```

这是Android NSD服务的内部错误，通常由以下几个原因引起：

## 解决方案

### 1. **修复NSD服务类型格式**

**问题**：服务类型格式不正确。在Android NSD中，服务类型应该以 `.local.` 结尾。

**修复**：
```java
// 修改 NsdHelper.java 中的服务类型
private static final String SERVICE_TYPE = "_autodroid._tcp.";
// 改为
private static final String SERVICE_TYPE = "_autodroid._tcp.local.";
```

### 2. **完整的NsdHelper修复**

```java
// NsdHelper.java
public class NsdHelper {
    private static final String TAG = "NsdHelper";
    private static final String SERVICE_TYPE = "_autodroid._tcp.local."; // 添加 .local.
    
    private Context context;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private ServiceDiscoveryCallback callback;
    
    // 添加重试机制
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    
    public NsdHelper(Context context, ServiceDiscoveryCallback callback) {
        this.context = context;
        this.callback = callback;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }
    
    public void initialize() {
        Log.d(TAG, "Initializing NsdHelper");
        
        // 检查网络权限
        if (!hasNetworkPermissions()) {
            Log.e(TAG, "Missing network permissions");
            if (callback != null) {
                callback.onDiscoveryFailed();
            }
            return;
        }
        
        initializeResolveListener();
        initializeDiscoveryListener();
        discoverServicesWithRetry();
    }
    
    private boolean hasNetworkPermissions() {
        // 检查网络权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing ACCESS_FINE_LOCATION permission");
                return false;
            }
        }
        return true;
    }
    
    private void discoverServicesWithRetry() {
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.e(TAG, "Max retry count reached, giving up");
            if (callback != null) {
                callback.onDiscoveryFailed();
            }
            return;
        }
        
        retryCount++;
        Log.d(TAG, "Discovery attempt " + retryCount + "/" + MAX_RETRY_COUNT);
        
        try {
            nsdManager.discoverServices(
                SERVICE_TYPE, 
                NsdManager.PROTOCOL_DNS_SD, 
                discoveryListener
            );
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid service type: " + SERVICE_TYPE, e);
            scheduleRetry();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start discovery", e);
            scheduleRetry();
        }
    }
    
    private void scheduleRetry() {
        // 指数退避重试
        long delay = (long) Math.pow(2, retryCount) * 1000;
        Log.d(TAG, "Scheduling retry in " + delay + "ms");
        
        retryHandler.postDelayed(() -> {
            if (retryCount < MAX_RETRY_COUNT) {
                discoverServicesWithRetry();
            }
        }, delay);
    }
    
    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed. Error code: " + errorCode + 
                      ", ServiceType: " + serviceType);
                
                // 根据错误码提供更详细的错误信息
                String errorMeaning = getErrorMeaning(errorCode);
                Log.e(TAG, "Error meaning: " + errorMeaning);
                
                // 重置重试计数以便重新尝试
                retryCount = 0;
                
                if (callback != null) {
                    callback.onDiscoveryFailed();
                }
            }
            
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop discovery failed: " + errorCode);
            }
            
            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Service discovery started successfully");
                retryCount = 0; // 重置重试计数
                if (callback != null) {
                    callback.onDiscoveryStarted();
                }
            }
            
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Service discovery stopped");
            }
            
            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo.getServiceName() + 
                      ", type: " + serviceInfo.getServiceType());
                
                // 验证服务类型
                String foundType = serviceInfo.getServiceType();
                if (!foundType.equals(SERVICE_TYPE) && 
                    !foundType.equals("_" + SERVICE_TYPE.replace(".local.", ""))) {
                    Log.d(TAG, "Unknown Service Type: " + foundType + 
                          ", expected: " + SERVICE_TYPE);
                    return;
                }
                
                // 解析服务
                nsdManager.resolveService(serviceInfo, resolveListener);
            }
            
            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
                if (callback != null) {
                    callback.onServiceLost(serviceInfo.getServiceName());
                }
            }
        };
    }
    
    private void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName() + 
                      ", error: " + errorCode);
            }
            
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolved service: " + serviceInfo);
                
                // 获取服务信息
                String serviceName = serviceInfo.getServiceName();
                InetAddress host = serviceInfo.getHost();
                int port = serviceInfo.getPort();
                
                if (host == null) {
                    Log.e(TAG, "Resolved service has null host");
                    return;
                }
                
                String hostAddress = host.getHostAddress();
                Log.d(TAG, "Service details - Name: " + serviceName + 
                      ", Host: " + hostAddress + ", Port: " + port);
                
                if (callback != null) {
                    callback.onServiceFound(serviceName, hostAddress, port);
                }
            }
        };
    }
    
    private String getErrorMeaning(int errorCode) {
        switch (errorCode) {
            case NsdManager.FAILURE_INTERNAL_ERROR:
                return "FAILURE_INTERNAL_ERROR";
            case NsdManager.FAILURE_ALREADY_ACTIVE:
                return "FAILURE_ALREADY_ACTIVE";
            case NsdManager.FAILURE_MAX_LIMIT:
                return "FAILURE_MAX_LIMIT";
            default:
                return "UNKNOWN_ERROR (" + errorCode + ")";
        }
    }
    
    public void stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
                retryHandler.removeCallbacksAndMessages(null);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping discovery", e);
            }
        }
    }
    
    // 清理方法
    public void tearDown() {
        stopDiscovery();
        retryHandler.removeCallbacksAndMessages(null);
    }
}
```

### 3. **权限检查与请求**

在你的BaseActivity或MainActivity中添加权限检查：

```java
// 权限请求码
private static final int REQUEST_PERMISSIONS = 1001;

private void checkAndRequestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        List<String> permissions = new ArrayList<>();
        
        // 网络发现需要的权限
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        
        if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE);
        }
        
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            startServiceDiscovery();
        }
    } else {
        startServiceDiscovery();
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                       @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (requestCode == REQUEST_PERMISSIONS) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            Log.d(TAG, "All permissions granted, starting discovery");
            startServiceDiscovery();
        } else {
            Log.e(TAG, "Some permissions were denied");
            Toast.makeText(this, "需要网络权限才能发现服务器", Toast.LENGTH_LONG).show();
        }
    }
}
```

### 4. **网络状态检查**

```java
private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    
    if (connectivityManager != null) {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    return false;
}

private boolean isWifiConnected() {
    ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    
    if (connectivityManager != null) {
        NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetwork != null && wifiNetwork.isConnectedOrConnecting();
    }
    return false;
}
```

### 5. **AndroidManifest.xml配置**

确保有以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 6. **测试步骤**

按以下顺序测试：

1. **检查权限**：确保应用有位置权限
2. **检查网络**：确保设备连接到WiFi网络
3. **检查服务类型**：确保服务类型格式正确（以`.local.`结尾）
4. **延迟启动**：在Activity的`onResume`中延迟启动发现：

```java
@Override
protected void onResume() {
    super.onResume();
    
    // 延迟启动网络发现，确保网络就绪
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (isWifiConnected()) {
            startServiceDiscovery();
        } else {
            Toast.makeText(this, "请连接到WiFi网络", Toast.LENGTH_LONG).show();
        }
    }, 1000);
}
```

### 7. **备选方案：使用第三方库**

如果Android原生NSD API持续有问题，可以考虑使用第三方库：

```gradle
// build.gradle
dependencies {
    implementation 'javax.jmdns:jmdns:3.4.1'
    // 或者
    implementation 'org.bitlet:weupnp:0.1.4'
}
```

**主要修改点总结：**
1. 服务类型从 `"_autodroid._tcp."` 改为 `"_autodroid._tcp.local."`
2. 添加完善的错误处理和重试机制
3. 确保权限检查和网络状态检查
4. 在合适的时机启动发现服务

这些修改应该能解决你的mDNS发现失败问题。