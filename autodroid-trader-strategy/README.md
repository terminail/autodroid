# Trader Strategy
交易策略根据市场量价信息创建交易指令tradeplan，并将tradeplan发送给Trader Server。

主要与Trader Server交互，主要功能包括：
0. 管理交易策略：包括创建、更新、删除交易策略
1. 收集处理市场量价信息：收集处理市场量价信息
2. 创建交易指令：交易策略根据市场量价信息创建交易指令tradeplan
3. 发送交易指令：将交易指令tradeplan发送给Trader Server
