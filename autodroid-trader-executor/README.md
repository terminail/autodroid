# autodroid-trader-executor
交易执行组件，根据tradeplan执行交易指令。

主要与autodroid-trader-server交互，主要功能包括：
1. 接收tradeplan：从autodroid-trader-server接收tradeplan
2. 执行交易指令：根据tradeplan执行交易指令，采用纯ADB执行具体的页面自动化操作
3. 报告执行结果：将交易执行结果报告给autodroid-trader-server

## 技术栈
1. adb：用于执行交易指令
2. python：用于编写交易执行组件
