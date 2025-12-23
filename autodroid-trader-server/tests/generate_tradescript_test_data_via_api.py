"""
TradeScript测试数据生成脚本

生成各种交易策略的TradeScript测试数据，包括：
1. 网格交易策略
2. RSI反转策略
3. 均线突破策略
4. 定投策略
5. 布林带策略
"""

import requests
import json
from typing import Dict, Any, List


API_BASE_URL = "http://localhost:8004/api"
TRADERIPTS_API = f"{API_BASE_URL}/tradescripts"


def create_tradescript(data: Dict[str, Any]) -> Dict[str, Any]:
    """创建TradeScript"""
    url = f"{API_BASE_URL}/tradescripts"
    response = requests.post(url, json=data)
    return response.json()


def generate_grid_trading_strategy() -> Dict[str, Any]:
    """生成网格交易策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "网格交易策略",
        "description": "在价格区间内设置多个买卖网格，通过价格波动获取收益",
        "metadata": {
            "strategy_type": "grid_trading",
            "version": "1.0.0",
            "parameters": {
                "grid_size": {
                    "type": "float",
                    "description": "网格大小（百分比）",
                    "default": 0.5,
                    "min": 0.1,
                    "max": 5.0
                },
                "grid_count": {
                    "type": "int",
                    "description": "网格数量",
                    "default": 10,
                    "min": 2,
                    "max": 50
                },
                "amount_per_grid": {
                    "type": "float",
                    "description": "每个网格的交易金额",
                    "default": 10000,
                    "min": 1000
                },
                "upper_bound": {
                    "type": "float",
                    "description": "价格上界",
                    "required": True
                },
                "lower_bound": {
                    "type": "float",
                    "description": "价格下界",
                    "required": True
                }
            },
            "risk_level": "medium",
            "suitable_for": ["stocks", "etf", "crypto"],
            "expected_return": "5-15% annually",
            "max_drawdown": "10-20%"
        },
        "script_path": "/scripts/grid_trading.py",
        "status": "OK"
    }


def generate_rsi_reversal_strategy() -> Dict[str, Any]:
    """生成RSI反转策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "RSI反转策略",
        "description": "基于相对强弱指数(RSI)的超买超卖反转交易策略",
        "metadata": {
            "strategy_type": "rsi_reversal",
            "version": "1.0.0",
            "parameters": {
                "rsi_period": {
                    "type": "int",
                    "description": "RSI周期",
                    "default": 14,
                    "min": 5,
                    "max": 30
                },
                "rsi_oversold": {
                    "type": "float",
                    "description": "超卖阈值（买入信号）",
                    "default": 30,
                    "min": 20,
                    "max": 40
                },
                "rsi_overbought": {
                    "type": "float",
                    "description": "超买阈值（卖出信号）",
                    "default": 70,
                    "min": 60,
                    "max": 80
                },
                "position_size": {
                    "type": "float",
                    "description": "仓位大小",
                    "default": 10000,
                    "min": 1000
                },
                "stop_loss": {
                    "type": "float",
                    "description": "止损百分比",
                    "default": 5.0,
                    "min": 1.0,
                    "max": 20.0
                }
            },
            "risk_level": "medium",
            "suitable_for": ["stocks", "crypto"],
            "expected_return": "10-25% annually",
            "max_drawdown": "15-30%"
        },
        "script_path": "/scripts/rsi_reversal.py",
        "status": "OK"
    }


def generate_ma_breakout_strategy() -> Dict[str, Any]:
    """生成均线突破策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "均线突破策略",
        "description": "当价格突破均线时执行买入操作的趋势跟踪策略",
        "metadata": {
            "strategy_type": "ma_breakout",
            "version": "1.0.0",
            "parameters": {
                "ma_period": {
                    "type": "int",
                    "description": "均线周期",
                    "default": 20,
                    "min": 5,
                    "max": 200
                },
                "ma_type": {
                    "type": "string",
                    "description": "均线类型",
                    "default": "SMA",
                    "options": ["SMA", "EMA"]
                },
                "breakout_threshold": {
                    "type": "float",
                    "description": "突破阈值（百分比）",
                    "default": 0.5,
                    "min": 0.1,
                    "max": 5.0
                },
                "position_size": {
                    "type": "float",
                    "description": "仓位大小",
                    "default": 20000,
                    "min": 1000
                },
                "trailing_stop": {
                    "type": "float",
                    "description": "移动止损百分比",
                    "default": 3.0,
                    "min": 1.0,
                    "max": 10.0
                }
            },
            "risk_level": "medium-high",
            "suitable_for": ["stocks", "etf", "crypto"],
            "expected_return": "15-30% annually",
            "max_drawdown": "20-40%"
        },
        "script_path": "/scripts/ma_breakout.py",
        "status": "OK"
    }


def generate_dca_strategy() -> Dict[str, Any]:
    """生成定投策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "定期定额投资策略",
        "description": "定期定额投资策略，通过分散时间降低市场波动风险",
        "metadata": {
            "strategy_type": "dca",
            "version": "1.0.0",
            "parameters": {
                "amount": {
                    "type": "float",
                    "description": "每次投资金额",
                    "default": 5000,
                    "min": 1000
                },
                "frequency": {
                    "type": "string",
                    "description": "投资频率",
                    "default": "monthly",
                    "options": ["daily", "weekly", "monthly", "quarterly"]
                },
                "investment_day": {
                    "type": "int",
                    "description": "每月投资日期（1-28）",
                    "default": 1,
                    "min": 1,
                    "max": 28
                },
                "auto_reinvest": {
                    "type": "boolean",
                    "description": "是否自动再投资收益",
                    "default": True
                }
            },
            "risk_level": "low",
            "suitable_for": ["stocks", "etf", "funds"],
            "expected_return": "8-12% annually",
            "max_drawdown": "10-20%"
        },
        "script_path": "/scripts/dca.py",
        "status": "OK"
    }


def generate_bollinger_bands_strategy() -> Dict[str, Any]:
    """生成布林带策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "布林带策略",
        "description": "基于布林带指标的均值回归交易策略",
        "metadata": {
            "strategy_type": "bollinger_bands",
            "version": "1.0.0",
            "parameters": {
                "period": {
                    "type": "int",
                    "description": "布林带周期",
                    "default": 20,
                    "min": 5,
                    "max": 50
                },
                "std_dev": {
                    "type": "float",
                    "description": "标准差倍数",
                    "default": 2.0,
                    "min": 1.0,
                    "max": 3.0
                },
                "position_size": {
                    "type": "float",
                    "description": "仓位大小",
                    "default": 15000,
                    "min": 1000
                },
                "take_profit": {
                    "type": "float",
                    "description": "止盈百分比",
                    "default": 3.0,
                    "min": 1.0,
                    "max": 10.0
                },
                "stop_loss": {
                    "type": "float",
                    "description": "止损百分比",
                    "default": 2.0,
                    "min": 0.5,
                    "max": 5.0
                }
            },
            "risk_level": "medium",
            "suitable_for": ["stocks", "etf", "crypto"],
            "expected_return": "12-20% annually",
            "max_drawdown": "15-25%"
        },
        "script_path": "/scripts/bollinger_bands.py",
        "status": "OK"
    }


def generate_macd_strategy() -> Dict[str, Any]:
    """生成MACD策略"""
    return {
        "apk_package": "com.autodroid.trader",
        "name": "MACD趋势策略",
        "description": "基于MACD指标的趋势跟踪策略",
        "metadata": {
            "strategy_type": "macd",
            "version": "1.0.0",
            "parameters": {
                "fast_period": {
                    "type": "int",
                    "description": "快线周期",
                    "default": 12,
                    "min": 5,
                    "max": 20
                },
                "slow_period": {
                    "type": "int",
                    "description": "慢线周期",
                    "default": 26,
                    "min": 15,
                    "max": 50
                },
                "signal_period": {
                    "type": "int",
                    "description": "信号线周期",
                    "default": 9,
                    "min": 5,
                    "max": 15
                },
                "position_size": {
                    "type": "float",
                    "description": "仓位大小",
                    "default": 25000,
                    "min": 1000
                },
                "trailing_stop": {
                    "type": "float",
                    "description": "移动止损百分比",
                    "default": 4.0,
                    "min": 1.0,
                    "max": 10.0
                }
            },
            "risk_level": "medium-high",
            "suitable_for": ["stocks", "etf", "crypto"],
            "expected_return": "15-35% annually",
            "max_drawdown": "20-35%"
        },
        "script_path": "/scripts/macd.py",
        "status": "OK"
    }


def main():
    """主函数：生成所有TradeScript测试数据"""
    strategies = [
        generate_grid_trading_strategy(),
        generate_rsi_reversal_strategy(),
        generate_ma_breakout_strategy(),
        generate_dca_strategy(),
        generate_bollinger_bands_strategy(),
        generate_macd_strategy()
    ]
    
    print("开始生成TradeScript测试数据...\n")
    
    created_scripts = []
    failed_scripts = []
    
    for strategy in strategies:
        try:
            print(f"正在创建策略: {strategy['name']}")
            response = create_tradescript(strategy)
            
            if response.get("tradescript"):
                script_id = response["tradescript"]["id"]
                print(f"  ✓ 成功创建，ID: {script_id}")
                created_scripts.append({
                    "name": strategy["name"],
                    "id": script_id,
                    "strategy_type": strategy["metadata"]["strategy_type"]
                })
            else:
                print(f"  ✗ 创建失败: {response.get('message', '未知错误')}")
                failed_scripts.append(strategy["name"])
                
        except Exception as e:
            print(f"  ✗ 创建失败: {str(e)}")
            failed_scripts.append(strategy["name"])
        
        print()
    
    print("=" * 60)
    print(f"生成完成！")
    print(f"成功创建: {len(created_scripts)} 个策略")
    print(f"失败: {len(failed_scripts)} 个策略")
    print("=" * 60)
    
    if created_scripts:
        print("\n创建的策略列表:")
        for script in created_scripts:
            print(f"  - {script['name']} (ID: {script['id']}, 类型: {script['strategy_type']})")
    
    if failed_scripts:
        print(f"\n失败的策略:")
        for name in failed_scripts:
            print(f"  - {name}")
    
    print("\nTradeScript ID列表（用于创建TradePlan时引用）:")
    for script in created_scripts:
        print(f"  {script['strategy_type']}: {script['id']}")


if __name__ == "__main__":
    main()
