
# Open-AutoGLM集成测试报告
生成时间: 2025-12-09 20:02:29
设备ID: emulator-5554

## 总体摘要

- 总测试用例数: 8
- 总成功用例数: 4
- 总匹配用例数: 0
- 总体成功率: 50.00%
- 总体匹配率: 0.00%


# Open-AutoGLM集成测试报告
生成时间: 2025-12-09 20:02:29
设备ID: emulator-5554

## 测试摘要


### AutoDroid Manager - intelligent_login

- 测试用例数: 3
- 成功用例数: 3
- 结果匹配数: 0
- 成功率: 100.00%
- 匹配率: 0.00%


### AutoDroid Manager - coordinate_login

- 测试用例数: 3
- 成功用例数: 0
- 结果匹配数: 0
- 成功率: 0.00%
- 匹配率: 0.00%


### Taobao - intelligent_login

- 测试用例数: 1
- 成功用例数: 1
- 结果匹配数: 0
- 成功率: 100.00%
- 匹配率: 0.00%


### Taobao - coordinate_login

- 测试用例数: 1
- 成功用例数: 0
- 结果匹配数: 0
- 成功率: 0.00%
- 匹配率: 0.00%


## 详细测试结果

### AutoDroid Manager - intelligent_login 详细结果

- **15317227@qq.com**: success (预期: success, 实际: failed, 匹配: False)
- **15317227@qq.com**: success (预期: failure, 实际: failed, 匹配: False)
- ****: success (预期: failure, 实际: failed, 匹配: False)

### AutoDroid Manager - coordinate_login 详细结果

- **15317227@qq.com**: failed (预期: success, 实际: unknown, 匹配: False)
  - 错误: 'NoneType' object has no attribute '_get_adb_prefix'
- **15317227@qq.com**: failed (预期: failure, 实际: unknown, 匹配: False)
  - 错误: 'NoneType' object has no attribute '_get_adb_prefix'
- ****: failed (预期: failure, 实际: unknown, 匹配: False)
  - 错误: 'NoneType' object has no attribute '_get_adb_prefix'

### Taobao - intelligent_login 详细结果

- **test_user**: success (预期: failure, 实际: failed, 匹配: False)

### Taobao - coordinate_login 详细结果

- **test_user**: failed (预期: failure, 实际: unknown, 匹配: False)
  - 错误: 'NoneType' object has no attribute '_get_adb_prefix'

