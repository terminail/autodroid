# AutoDroid Trader IME

这是一个自定义的输入法应用，用于在 Android 设备上进行自动化交易操作。该应用包含一个最小化的输入法实现和一个 AccessibilityService 用于获取页面 UI 结构。

Finally , AutoDroid Trader IME will be integrated into AutoDroid Trader App.

## 功能

1. **自定义输入法 (TraderIME)**：一个最小化的输入法实现，不会弹出键盘界面，避免与自动化操作冲突。
2. **AccessibilityService (TraderAccessibilityService)**：用于获取当前应用的 UI 层次结构，提供比 dumpsys 更详细的页面信息。

## 安装

1. 确保已连接 Android 设备并启用 USB 调试
2. 运行安装脚本：
   ```
   install_ime.bat
   ```
3. 在设备上启用输入法：
   - 设置 -> 系统和更新 -> 语言和输入法 -> 当前输入法 -> AutoDroid Trader IME
4. 在设备上启用辅助功能服务：
   - 设置 -> 辅助功能 -> AutoDroid Trader Accessibility Service

## 使用

安装并启用后，系统将使用此输入法作为默认输入法，且不会弹出键盘界面。AccessibilityService 将自动运行并收集页面信息。

## 注意事项

- 需要授予应用辅助功能权限才能正常工作
- 此应用旨在最小化对系统的影响，仅提供必要的功能