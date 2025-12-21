# Features

## Wifi 局域网内扫描发现服务器
根据手机开启wifi情况下，自动扫描局域网内的服务器，自动根据连接的wifi网络，扫描出可以连接的服务器。具体实现如下：
1. 检查手机是否开启wifi功能。如果没有开启，提示用户开启wifi功能，转到手机系统WiFi开启设置界面。
2. 如果wifi功能已开启，首先尝试连接数据库中已保存的服务器：
    1. 从数据库中获取所有已保存的服务器信息。
    2. 依次尝试访问每个服务器的API端点。
    3. 如果找到可用的服务器，直接连接并更新服务器信息。
    4. 如果所有已保存的服务器都不可用，则启动局域网扫描。
3. 启动局域网扫描，扫描局域网内的服务器。具体实现如下：
    1. 获取当前连接的wifi网络SSID。
    2. 扫描局域网内的所有IP地址。
    3. 对每个IP地址，发送一个ICMP请求包（也称为ping请求）。
    4. 如果收到ICMP回复包（也称为ping回复），则认为该IP地址对应的设备可能是一个服务器。
4. 根据扫描到的可能服务器的IP地址，筛选出可以连接的服务器。具体实现如下：
    1. 对每个可能的服务器IP地址，从端口8000开始直至端口9000，访问api：`http://<IP地址>:<端口>/api/server`。
    2. 如果收到HTTP 200响应包，则认为该<IP地址>:<端口>对应的设备就是要找的服务器，记录下该IP地址和端口号。
5. 在扫描过程中，实时显示扫描进度和结果。
    1. 访问api：`http://<IP地址>:<端口>/api/server`
    2. 每访问一次api，就显示一次扫描结果，包括IP地址、端口号、返回成功与否信息，并更新DashboardViewModel，以便ItemServer订阅显示扫描进度信息在应用界面上。
    3. 访问api成功返回，立即停止扫描，将获取到的服务器信息保存到数据库。
    4. 支持暂停/恢复扫描功能：用户可以通过点击自动扫描按钮来暂停正在进行的扫描，再次点击可以恢复扫描。扫描状态会在界面上实时显示。

## 自动获取并保存Device信息
    1. 应用启动时，AppViewModel 会自动初始化并获取设备信息保存在本地数据库中。
    2. DeviceManager 使用设备序列号 (Build.SERIAL) 作为设备唯一标识符，确保与终端显示的序列号一致。
    3. 如果设备序列号不可用，系统会自动使用 Android ID 作为备用标识符。
    4. 自动收集的设备信息包括：设备ID、设备名称、型号、制造商、Android版本、本地IP地址等详细信息。
    5. 遵循 Local-First 设计原则，设备信息优先保存在本地数据库，确保离线状态下也能正常访问。
    6. 添加详细的日志记录，便于调试和追踪设备初始化过程。

## 向服务器注册Device信息
    1. 当用户打开Dashboard界面点击注册Device按钮， ServerManager 会调用ServerRepository的registerDevice方法，将DeviceRegistrationRequest信息发送到服务器。
    2. 服务器收到DeviceRegistrationRequest注册请求后，会验证Device注册信息的有效性。
    3. 如果Device注册信息有效，服务器会返回一个成功的响应DeviceRegistrationResponse， ServerManager 会显示成功注册信息给用户。**同时服务器会自动尝试扫描该Device的是否安装服务器支持的APK并更新服务器Device信息，等待下次app查询获取Device最新信息。**
    4. 如果Device注册信息无效，服务器会返回一个错误响应ErrorResponse， ServerManager 会显示错误信息给用户。
    5. 注册成功后， ServerManager 会更新本地数据库中的DeviceEntity信息。

## 从服务器获取Device信息
    1. 当用户打开Dashboard界面， ServerManager 会订阅device livedata。
    2. 服务器会返回一个成功的响应DeviceInfoResponse， ServerManager 会显示Device信息给用户。
