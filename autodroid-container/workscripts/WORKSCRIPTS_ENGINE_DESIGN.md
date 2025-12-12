# workscripts 引擎设计 (ADB-Based)
## 工作原理
workscript engine 是一个基于 python 的脚本引擎，用于执行 workscripts 中的脚本。**完全基于ADB命令实现，已移除Appium依赖。**

Engine读取数据库中的workplan，按照workplan创建时间顺序执行workscripts中的脚本。workplan中有workscript需要的参数，engine会将参数传递给workscript。workscript执行完成后，engine会将执行结果写入数据库。

所以所有编写的workscript都需要符合一定的规范，才能被workscript engine执行。

参见 [5.1 核心数据模型关系](../DESIGN.md)部分，workplan的data字段是json格式，workscript需要的参数都在这个字段中。

由于workscript可能很多，所以engine需要能够动态加载workscript。根据workplan中的workscript信息，engine会在workscripts目录中查找对应的脚本文件。

工作流程：
1. engine从数据库中读取workplan，按照创建时间顺序执行。
2. 对于每个workplan，engine会根据workscript信息，在workscripts目录中查找对应的脚本文件。
3. 如果找到对应的脚本文件，engine会加载脚本文件，并将workplan中的参数传递给脚本初始化。
4. Engine执行脚本，脚本会将报告生成在配置的reports目录中。
5. 脚本执行完成后，engine会将执行结果更新workplan状态。

**ADB-Based 优势：**
- 直接设备控制，无Appium服务器开销
- 更快的执行速度和响应时间
- 更简单的架构，减少故障点
- 更好的设备兼容性

## 脚本规范
1. 脚本文件必须以.py结尾。
2. 脚本文件必须在workscripts目录中。
3. 脚本文件必须包含一个类，类名必须与文件名相同。
4. 类必须包含一个初始化方法__init__，方法参数必须包含workplan参数。
5. 类必须包含一个run方法，方法参数必须包含workplan参数。
6. run方法必须返回一个字典，字典中包含workscript执行结果。
7. 字典中必须包含一个key为status，value为workscript执行状态的字符串。
8. 字典中可以包含其他key-value对，用于存储workscript执行结果。



