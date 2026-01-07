# AutoDroid Trader IME

这是一个自定义的输入法应用，用于在 Android 设备上进行自动化交易操作。该应用包含一个最小化的输入法实现和一个 AccessibilityService 用于获取页面 UI 结构。

Finally , AutoDroid Trader IME will be integrated into AutoDroid Trader App.

## 功能

1. **自定义输入法 (TraderIME)**：一个最小化的输入法实现，不会弹出键盘界面，避免与自动化操作冲突。
2. **AccessibilityService (TraderImeService)**：用于获取当前应用的 UI 层次结构，提供比 dumpsys 更详细的页面信息。
3. **ADB Intent Support**：支持通过 ADB 发送广播意图来触发无障碍服务的 UI 结构导出。

## 安装

1. 确保已连接 Android 设备并启用 USB 调试
2. 运行安装脚本：
   ```
   install_ime.bat
   ```
3. 在设备上启用输入法：
   - 设置 -> 系统和更新 -> 语言和输入法 -> 当前输入法 -> AutoDroid Trader IME
4. 在设备上启用辅助功能服务：
   - 设置 -> 辅助功能 -> Trader IME

## 使用

### 基本使用
安装并启用后，系统将使用此输入法作为默认输入法，且不会弹出键盘界面。AccessibilityService 将自动运行并收集页面信息。

### 通过 ADB 发送意图触发 UI 结构导出
您可以使用 ADB 命令发送广播意图来触发无障碍服务导出当前 UI 结构：

```
adb shell am broadcast -a com.autodroid.trader.ACCESSIBILITY_DUMP_REQUEST -n com.autodroid.trader.ime/.AccessibilityDumpReceiver
```

执行此命令后，服务将在应用的内部存储空间中创建 `trader_ime_dump.json` 文件，包含当前界面的完整 UI 结构信息。文件保存在 `/data/user/0/com.autodroid.trader.ime/files/autodroid_dumps/trader_ime_dump.json`。

### 获取导出的 UI 结构文件
要获取导出的 UI 结构文件到本地机器，请使用以下 ADB 命令：
```
adb shell "run-as com.autodroid.trader.ime cat /data/user/0/com.autodroid.trader.ime/files/autodroid_dumps/trader_ime_dump.json" > trader_ime_dump.json
```

## 注意事项

- 需要授予应用辅助功能权限才能正常工作
- 此应用旨在最小化对系统的影响，仅提供必要的功能
- 无障碍服务需要在系统设置中手动启用
- UI 结构导出文件会覆盖之前的文件，如需保存历史数据请重命名或移动文件