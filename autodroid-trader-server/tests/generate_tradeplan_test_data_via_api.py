#!/usr/bin/env python3
"""
通过API调用生成交易计划测试数据
包含真实市场数据（OHLCV、涨跌幅等）并关联TradeScript策略
"""

import requests
import json
from typing import Dict, Any, List, Optional


API_BASE_URL = "http://localhost:8004/api"
TRADEPLANS_API = f"{API_BASE_URL}/tradeplans"
TRADERIPTS_API = f"{API_BASE_URL}/tradescripts"


def get_all_tradescripts() -> Dict[str, str]:
    """获取所有TradeScript并返回策略类型到ID的映射"""
    try:
        response = requests.get(TRADERIPTS_API)
        if response.status_code == 200:
            data = response.json()
            tradescripts = data.get("tradescripts", [])
            
            script_map = {}
            for ts in tradescripts:
                strategy_type = ts.get("metadata", {}).get("strategy_type")
                if strategy_type:
                    script_map[strategy_type] = ts["id"]
            
            return script_map
        else:
            print(f"获取TradeScript失败: {response.status_code}")
            return {}
    except Exception as e:
        print(f"获取TradeScript异常: {e}")
        return {}


def generate_market_data(symbol, name, description, open_price, high_price, low_price, close_price, volume, change_percent):
    """生成市场数据"""
    return {
        "symbol": symbol,
        "name": name,
        "description": description,
        "ohlcv": {
            "open": open_price,
            "high": high_price,
            "low": low_price,
            "close": close_price,
            "volume": volume
        },
        "change_percent": change_percent,
        "timestamp": "2025-12-23T09:30:00Z"
    }


def generate_test_tradeplans(script_map: Dict[str, str]) -> List[Dict[str, Any]]:
    """生成测试交易计划数据，关联TradeScript"""
    
    tradeplans = []
    
    # 定义更多真实市场数据
    market_data = [
        # 港股
        {"exchange": "HKEX", "symbol": "00700.HK", "name": "腾讯控股", "open": 398.50, "high": 405.20, "low": 395.80, "close": 402.30, "volume": 15800000, "change": 1.25},
        {"exchange": "HKEX", "symbol": "00941.HK", "name": "中国移动", "open": 72.50, "high": 73.20, "low": 71.80, "close": 72.90, "volume": 8500000, "change": 0.55},
        {"exchange": "HKEX", "symbol": "01299.HK", "name": "友邦保险", "open": 68.20, "high": 69.50, "low": 67.80, "close": 68.90, "volume": 12000000, "change": 1.02},
        {"exchange": "HKEX", "symbol": "03690.HK", "name": "美团", "open": 125.80, "high": 128.50, "low": 124.20, "close": 126.50, "volume": 25000000, "change": 0.56},
        {"exchange": "HKEX", "symbol": "09618.HK", "name": "京东集团", "open": 108.50, "high": 112.30, "low": 107.20, "close": 110.80, "volume": 18000000, "change": 2.12},
        {"exchange": "HKEX", "symbol": "09888.HK", "name": "阿里巴巴", "open": 78.50, "high": 80.20, "low": 77.80, "close": 79.60, "volume": 22000000, "change": 1.40},
        {"exchange": "HKEX", "symbol": "02318.HK", "name": "中国平安", "open": 42.30, "high": 43.50, "low": 41.80, "close": 42.90, "volume": 35000000, "change": 1.43},
        {"exchange": "HKEX", "symbol": "00388.HK", "name": "港交所", "open": 298.50, "high": 305.20, "low": 295.80, "close": 302.30, "volume": 5800000, "change": 1.27},
        {"exchange": "HKEX", "symbol": "01024.HK", "name": "快手", "open": 58.50, "high": 60.20, "low": 57.80, "close": 59.30, "volume": 15000000, "change": 1.37},
        {"exchange": "HKEX", "symbol": "02020.HK", "name": "安踏体育", "open": 88.50, "high": 90.20, "low": 87.80, "close": 89.60, "volume": 8500000, "change": 1.25},
        
        # 美股科技
        {"exchange": "NASDAQ", "symbol": "AAPL", "name": "苹果公司", "open": 193.20, "high": 195.50, "low": 192.10, "close": 194.80, "volume": 52000000, "change": 0.98},
        {"exchange": "NASDAQ", "symbol": "MSFT", "name": "微软", "open": 374.80, "high": 378.50, "low": 372.30, "close": 376.20, "volume": 22000000, "change": 0.42},
        {"exchange": "NASDAQ", "symbol": "GOOGL", "name": "谷歌", "open": 141.50, "high": 144.20, "low": 140.30, "close": 143.80, "volume": 18000000, "change": 1.72},
        {"exchange": "NASDAQ", "symbol": "META", "name": "Meta", "open": 505.30, "high": 512.80, "low": 502.50, "close": 510.60, "volume": 15000000, "change": 1.15},
        {"exchange": "NASDAQ", "symbol": "AMZN", "name": "亚马逊", "open": 178.50, "high": 182.30, "low": 176.80, "close": 180.20, "volume": 45000000, "change": 0.95},
        {"exchange": "NASDAQ", "symbol": "TSLA", "name": "特斯拉", "open": 418.50, "high": 425.30, "low": 412.80, "close": 420.15, "volume": 98000000, "change": 0.52},
        {"exchange": "NASDAQ", "symbol": "NVDA", "name": "英伟达", "open": 495.30, "high": 502.80, "low": 490.50, "close": 498.60, "volume": 45000000, "change": 0.78},
        {"exchange": "NASDAQ", "symbol": "AMD", "name": "AMD", "open": 145.80, "high": 150.20, "low": 144.50, "close": 148.30, "volume": 68000000, "change": 1.71},
        {"exchange": "NASDAQ", "symbol": "NFLX", "name": "奈飞", "open": 628.50, "high": 635.20, "low": 622.80, "close": 631.90, "volume": 5200000, "change": 0.54},
        {"exchange": "NASDAQ", "symbol": "ADBE", "name": "Adobe", "open": 548.50, "high": 555.20, "low": 545.80, "close": 552.30, "volume": 2800000, "change": 0.69},
        
        # 美股传统
        {"exchange": "NYSE", "symbol": "JPM", "name": "摩根大通", "open": 198.50, "high": 202.30, "low": 196.80, "close": 200.60, "volume": 8500000, "change": 1.05},
        {"exchange": "NYSE", "symbol": "BAC", "name": "美国银行", "open": 35.80, "high": 36.50, "low": 35.20, "close": 36.10, "volume": 45000000, "change": 0.84},
        {"exchange": "NYSE", "symbol": "WMT", "name": "沃尔玛", "open": 162.50, "high": 165.20, "low": 161.80, "close": 164.30, "volume": 6800000, "change": 1.11},
        {"exchange": "NYSE", "symbol": "KO", "name": "可口可乐", "open": 59.80, "high": 60.50, "low": 59.20, "close": 60.10, "volume": 12000000, "change": 0.50},
        {"exchange": "NYSE", "symbol": "PG", "name": "宝洁", "open": 158.50, "high": 160.20, "low": 157.80, "close": 159.60, "volume": 5800000, "change": 0.69},
        {"exchange": "NYSE", "symbol": "XOM", "name": "埃克森美孚", "open": 112.50, "high": 115.20, "low": 111.80, "close": 114.30, "volume": 15000000, "change": 1.60},
        {"exchange": "NYSE", "symbol": "CVX", "name": "雪佛龙", "open": 158.80, "high": 162.50, "low": 157.20, "close": 160.50, "volume": 8500000, "change": 1.07},
        {"exchange": "NYSE", "symbol": "JNJ", "name": "强生", "open": 158.50, "high": 160.20, "low": 157.80, "close": 159.60, "volume": 6800000, "change": 0.69},
        {"exchange": "NYSE", "symbol": "V", "name": "Visa", "open": 278.50, "high": 282.20, "low": 276.80, "close": 280.60, "volume": 5800000, "change": 0.75},
        {"exchange": "NYSE", "symbol": "DIS", "name": "迪士尼", "open": 112.50, "high": 115.20, "low": 111.80, "close": 114.30, "volume": 12000000, "change": 1.60},
        
        # ETF
        {"exchange": "NASDAQ", "symbol": "QQQ", "name": "纳斯达克100 ETF", "open": 512.30, "high": 518.50, "low": 510.20, "close": 516.80, "volume": 35000000, "change": 0.95},
        {"exchange": "NYSE", "symbol": "SPY", "name": "标普500 ETF", "open": 478.50, "high": 482.20, "low": 476.80, "close": 480.60, "volume": 85000000, "change": 0.44},
        {"exchange": "NYSE", "symbol": "IWM", "name": "罗素2000 ETF", "open": 198.50, "high": 202.20, "low": 196.80, "close": 200.60, "volume": 45000000, "change": 1.05},
        {"exchange": "NYSE", "symbol": "DIA", "name": "道琼斯ETF", "open": 378.50, "high": 382.20, "low": 376.80, "close": 380.60, "volume": 25000000, "change": 0.55},
        {"exchange": "NYSE", "symbol": "GLD", "name": "黄金ETF", "open": 188.50, "high": 190.20, "low": 187.80, "close": 189.60, "volume": 12000000, "change": 0.58},
        {"exchange": "NYSE", "symbol": "SLV", "name": "白银ETF", "open": 22.50, "high": 23.20, "low": 22.20, "close": 22.80, "volume": 35000000, "change": 1.33},
        {"exchange": "NYSE", "symbol": "XLE", "name": "能源ETF", "open": 88.50, "high": 90.20, "low": 87.80, "close": 89.60, "volume": 25000000, "change": 1.25},
        {"exchange": "NYSE", "symbol": "XLK", "name": "科技ETF", "open": 168.50, "high": 172.20, "low": 167.80, "close": 170.60, "volume": 18000000, "change": 1.23},
        {"exchange": "NYSE", "symbol": "XLF", "name": "金融ETF", "open": 38.50, "high": 39.20, "low": 38.20, "close": 38.80, "volume": 35000000, "change": 0.78},
        {"exchange": "NYSE", "symbol": "XLV", "name": "医疗ETF", "open": 148.50, "high": 150.20, "low": 147.80, "close": 149.60, "volume": 12000000, "change": 0.74},
        
        # 加密货币
        {"exchange": "CRYPTO", "symbol": "BTC", "name": "比特币", "open": 95800.00, "high": 97150.00, "low": 95200.00, "close": 96500.00, "volume": 28000000000, "change": 0.85},
        {"exchange": "CRYPTO", "symbol": "ETH", "name": "以太坊", "open": 3580.00, "high": 3650.00, "low": 3540.00, "close": 3620.00, "volume": 15000000000, "change": 1.25},
        {"exchange": "CRYPTO", "symbol": "BNB", "name": "币安币", "open": 628.50, "high": 642.20, "low": 622.80, "close": 635.90, "volume": 1800000000, "change": 1.18},
        {"exchange": "CRYPTO", "symbol": "SOL", "name": "Solana", "open": 188.50, "high": 195.20, "low": 185.80, "close": 192.30, "volume": 3500000000, "change": 2.02},
        {"exchange": "CRYPTO", "symbol": "XRP", "name": "瑞波币", "open": 2.50, "high": 2.62, "low": 2.45, "close": 2.58, "volume": 2800000000, "change": 3.20},
        {"exchange": "CRYPTO", "symbol": "ADA", "name": "艾达币", "open": 1.15, "high": 1.22, "low": 1.12, "close": 1.18, "volume": 850000000, "change": 2.61},
        {"exchange": "CRYPTO", "symbol": "DOGE", "name": "狗狗币", "open": 0.38, "high": 0.42, "low": 0.36, "close": 0.40, "volume": 2500000000, "change": 5.26},
        {"exchange": "CRYPTO", "symbol": "DOT", "name": "波卡", "open": 7.50, "high": 7.82, "low": 7.35, "close": 7.68, "volume": 450000000, "change": 2.40},
        {"exchange": "CRYPTO", "symbol": "MATIC", "name": "Polygon", "open": 0.85, "high": 0.92, "low": 0.82, "close": 0.88, "volume": 680000000, "change": 3.53},
        {"exchange": "CRYPTO", "symbol": "LINK", "name": "Chainlink", "open": 25.50, "high": 26.82, "low": 25.20, "close": 26.30, "volume": 580000000, "change": 3.14},
        
        # 期货
        {"exchange": "COMEX", "symbol": "GC", "name": "黄金期货", "open": 2625.50, "high": 2640.30, "low": 2615.20, "close": 2632.80, "volume": 125000, "change": 0.32},
        {"exchange": "COMEX", "symbol": "SI", "name": "白银期货", "open": 30.50, "high": 31.20, "low": 30.20, "close": 30.80, "volume": 85000, "change": 0.98},
        {"exchange": "NYMEX", "symbol": "CL", "name": "原油期货", "open": 71.50, "high": 73.20, "low": 70.80, "close": 72.30, "volume": 250000, "change": 1.12},
        {"exchange": "CBOT", "symbol": "ZC", "name": "玉米期货", "open": 478.50, "high": 485.20, "low": 475.80, "close": 482.30, "volume": 180000, "change": 0.79},
        {"exchange": "CBOT", "symbol": "ZW", "name": "小麦期货", "open": 598.50, "high": 608.20, "low": 595.80, "close": 603.30, "volume": 120000, "change": 0.80},
        {"exchange": "CBOT", "symbol": "ZS", "name": "大豆期货", "open": 1288.50, "high": 1305.20, "low": 1282.80, "close": 1298.30, "volume": 150000, "change": 0.76},
        {"exchange": "CME", "symbol": "ES", "name": "标普500期货", "open": 4785.50, "high": 4822.20, "low": 4768.80, "close": 4806.30, "volume": 1500000, "change": 0.43},
        {"exchange": "CME", "symbol": "NQ", "name": "纳斯达克100期货", "open": 18525.50, "high": 18722.20, "low": 18468.80, "close": 18606.30, "volume": 850000, "change": 0.43},
        {"exchange": "CME", "symbol": "YM", "name": "道琼斯期货", "open": 37855.50, "high": 38222.20, "low": 37688.80, "close": 38063.30, "volume": 250000, "change": 0.55},
        {"exchange": "CME", "symbol": "RTY", "name": "罗素2000期货", "open": 1985.50, "high": 2022.20, "low": 1968.80, "close": 2006.30, "volume": 450000, "change": 1.05},
        
        # A股
        {"exchange": "SSE", "symbol": "600519.SH", "name": "贵州茅台", "open": 1685.50, "high": 1722.20, "low": 1668.80, "close": 1706.30, "volume": 2500000, "change": 1.23},
        {"exchange": "SSE", "symbol": "600036.SH", "name": "招商银行", "open": 32.50, "high": 33.20, "low": 32.20, "close": 32.80, "volume": 45000000, "change": 0.92},
        {"exchange": "SSE", "symbol": "601318.SH", "name": "中国平安", "open": 42.30, "high": 43.50, "low": 41.80, "close": 42.90, "volume": 85000000, "change": 1.43},
        {"exchange": "SSE", "symbol": "600000.SH", "name": "浦发银行", "open": 7.50, "high": 7.82, "low": 7.35, "close": 7.68, "volume": 85000000, "change": 2.40},
        {"exchange": "SZSE", "symbol": "000858.SZ", "name": "五粮液", "open": 148.50, "high": 152.20, "low": 147.80, "close": 150.60, "volume": 18000000, "change": 1.41},
        {"exchange": "SZSE", "symbol": "000001.SZ", "name": "平安银行", "open": 10.50, "high": 10.82, "low": 10.35, "close": 10.68, "volume": 68000000, "change": 1.71},
        {"exchange": "SZSE", "symbol": "002594.SZ", "name": "比亚迪", "open": 258.50, "high": 265.20, "low": 256.80, "close": 262.30, "volume": 35000000, "change": 1.47},
        {"exchange": "SZSE", "symbol": "300750.SZ", "name": "宁德时代", "open": 188.50, "high": 195.20, "low": 185.80, "close": 192.30, "volume": 28000000, "change": 2.02},
        {"exchange": "SZSE", "symbol": "002415.SZ", "name": "海康威视", "open": 32.50, "high": 33.82, "low": 32.20, "close": 33.30, "volume": 45000000, "change": 2.46},
        {"exchange": "SZSE", "symbol": "000333.SZ", "name": "美的集团", "open": 62.50, "high": 64.20, "low": 61.80, "close": 63.30, "volume": 25000000, "change": 1.28},
        
        # 其他
        {"exchange": "HKEX", "symbol": "3033.HK", "name": "恒生科技ETF", "open": 5.85, "high": 5.92, "low": 5.78, "close": 5.88, "volume": 85000000, "change": 0.68},
        {"exchange": "HKEX", "symbol": "02800.HK", "name": "恒生ETF", "open": 285.50, "high": 288.20, "low": 284.80, "close": 286.80, "volume": 15000000, "change": 0.45},
        {"exchange": "HKEX", "symbol": "08231.HK", "name": "南方东英恒生科技", "open": 5.50, "high": 5.62, "low": 5.45, "close": 5.58, "volume": 25000000, "change": 1.45},
        {"exchange": "HKEX", "symbol": "08288.HK", "name": "南方东英恒生高股息", "open": 12.50, "high": 12.82, "low": 12.35, "close": 12.68, "volume": 8500000, "change": 1.44},
        {"exchange": "HKEX", "symbol": "03009.HK", "name": "中国联通", "open": 6.50, "high": 6.82, "low": 6.45, "close": 6.68, "volume": 45000000, "change": 2.77},
        {"exchange": "HKEX", "symbol": "00762.HK", "name": "中国联通", "open": 6.50, "high": 6.82, "low": 6.45, "close": 6.68, "volume": 45000000, "change": 2.77},
        {"exchange": "HKEX", "symbol": "00939.HK", "name": "建设银行", "open": 5.50, "high": 5.62, "low": 5.45, "close": 5.58, "volume": 85000000, "change": 1.45},
        {"exchange": "HKEX", "symbol": "03988.HK", "name": "中国银行", "open": 3.50, "high": 3.62, "low": 3.45, "close": 3.58, "volume": 120000000, "change": 2.29},
        {"exchange": "HKEX", "symbol": "01288.HK", "name": "农业银行", "open": 3.20, "high": 3.32, "low": 3.15, "close": 3.28, "volume": 150000000, "change": 2.50},
        {"exchange": "HKEX", "symbol": "03968.HK", "name": "招商银行", "open": 32.50, "high": 33.20, "low": 32.20, "close": 32.80, "volume": 45000000, "change": 0.92},
    ]
    
    # 策略配置
    strategy_configs = {
        "grid_trading": {
            "name": "网格交易策略",
            "description": "使用网格交易策略在{symbol_name}上进行交易",
            "data": {
                "grid_size": 0.5,
                "grid_count": 10,
                "amount_per_grid": 10000,
                "upper_bound": 420.0,
                "lower_bound": 380.0
            }
        },
        "rsi_reversal": {
            "name": "RSI反转策略",
            "description": "基于RSI指标的反转交易策略",
            "data": {
                "rsi_oversold": 30,
                "rsi_overbought": 70,
                "rsi_period": 14,
                "position_size": 15000,
                "stop_loss": 5.0
            }
        },
        "ma_breakout": {
            "name": "均线突破策略",
            "description": "当价格突破均线时执行买入操作",
            "data": {
                "ma_period": 20,
                "ma_type": "SMA",
                "position_size": 20000,
                "breakout_threshold": 0.5,
                "trailing_stop": 3.0
            }
        },
        "dca": {
            "name": "定期定额投资策略",
            "description": "定期定额投资{symbol_name}",
            "data": {
                "amount": 5000,
                "frequency": "monthly",
                "investment_day": 1,
                "auto_reinvest": True
            }
        },
        "bollinger_bands": {
            "name": "布林带策略",
            "description": "价格突破布林带上轨时买入",
            "data": {
                "period": 20,
                "std_dev": 2.0,
                "position_size": 18000,
                "take_profit": 3.0,
                "stop_loss": 2.0
            }
        },
        "macd": {
            "name": "MACD趋势策略",
            "description": "基于MACD指标的趋势跟踪策略",
            "data": {
                "fast_period": 12,
                "slow_period": 26,
                "signal_period": 9,
                "position_size": 25000,
                "trailing_stop": 4.0
            }
        }
    }
    
    # 状态列表
    statuses = ["PENDING", "APPROVED", "REJECTED", "EXECUTING", "COMPLETED", "FAILED"]
    
    # 为每个市场数据生成多个交易计划
    plan_index = 0
    for market in market_data:
        for strategy_type, config in strategy_configs.items():
            script_id = script_map.get(strategy_type)
            if not script_id:
                continue
            
            # 为每个策略和市场组合生成2个不同状态的交易计划
            for i in range(2):
                status = statuses[(plan_index + i) % len(statuses)]
                
                tradeplan = {
                    "script_id": script_id,
                    "name": f"{config['name']} - {market['name']}",
                    "description": config['description'].format(symbol_name=market['name']),
                    "exchange": market['exchange'],
                    "symbol": market['symbol'],
                    "symbol_name": market['name'],
                    "ohlcv": {
                        "open": market['open'],
                        "high": market['high'],
                        "low": market['low'],
                        "close": market['close'],
                        "volume": market['volume']
                    },
                    "change_percent": market['change'],
                    "data": config['data'].copy(),
                    "status": status
                }
                
                tradeplans.append(tradeplan)
                plan_index += 1
                
                # 限制总数为120
                if len(tradeplans) >= 120:
                    return tradeplans[:120]
    
    return tradeplans


def clear_all_tradeplans():
    """清除所有交易计划"""
    try:
        response = requests.get(TRADEPLANS_API)
        if response.status_code == 200:
            data = response.json()
            tradeplans = data.get("tradeplans", [])
            
            deleted_count = 0
            for tp in tradeplans:
                tp_id = tp.get("id")
                if tp_id:
                    delete_response = requests.delete(f"{TRADEPLANS_API}/{tp_id}")
                    if delete_response.status_code == 200:
                        deleted_count += 1
            
            print(f"已清除 {deleted_count} 个现有交易计划")
            return deleted_count
        else:
            print(f"获取交易计划失败: {response.status_code}")
            return 0
    except Exception as e:
        print(f"清除交易计划失败: {e}")
        return 0


def create_tradeplan_via_api(tradeplan_data: Dict[str, Any]) -> bool:
    """通过API创建单个交易计划"""
    try:
        response = requests.post(
            TRADEPLANS_API,
            json=tradeplan_data,
            headers={"Content-Type": "application/json"}
        )
        return response.status_code == 200
    except Exception as e:
        print(f"API调用失败: {e}")
        return False


def generate_tradeplan_test_data():
    """通过API生成交易计划测试数据"""
    
    # 获取TradeScript映射
    print("正在获取TradeScript策略...")
    script_map = get_all_tradescripts()
    
    if not script_map:
        print("警告: 没有找到任何TradeScript策略")
        print("请先运行 generate_tradescript_test_data_via_api.py 生成TradeScript数据")
        return 0
    
    print(f"找到 {len(script_map)} 个策略:")
    for strategy_type, script_id in script_map.items():
        print(f"  - {strategy_type}: {script_id}")
    print()
    
    # 生成测试数据
    test_tradeplans = generate_test_tradeplans(script_map)
    
    if not test_tradeplans:
        print("警告: 没有生成任何交易计划数据")
        return 0
    
    created_count = 0
    failed_count = 0
    
    print("\n开始通过API生成交易计划测试数据...")
    print("=" * 80)
    
    for i, test_plan in enumerate(test_tradeplans, 1):
        try:
            success = create_tradeplan_via_api(test_plan)
            
            if success:
                print(f"[{i}/{len(test_tradeplans)}] 成功创建: {test_plan['name']}")
                print(f"    代码: {test_plan['symbol']} | 名称: {test_plan['symbol_name']}")
                print(f"    收盘价: {test_plan['ohlcv']['close']:.2f} | 涨跌幅: {test_plan['change_percent']:+.2f}%")
                print(f"    策略ID: {test_plan['script_id']}")
                print(f"    状态: {test_plan['status']}")
                print()
                created_count += 1
            else:
                print(f"[{i}/{len(test_tradeplans)}] 创建失败: {test_plan['name']}")
                failed_count += 1
                
        except Exception as e:
            print(f"[{i}/{len(test_tradeplans)}] 创建异常: {test_plan['name']} - {str(e)}")
            failed_count += 1
    
    print("=" * 80)
    print(f"\n生成完成！")
    print(f"成功: {created_count} 个")
    print(f"失败: {failed_count} 个")
    
    return created_count


def display_tradeplan_summary():
    """显示交易计划统计摘要"""
    try:
        response = requests.get(TRADEPLANS_API)
        if response.status_code == 200:
            data = response.json()
            tradeplans = data.get("tradeplans", [])
            
            print("\n" + "=" * 80)
            print("交易计划统计摘要")
            print("=" * 80)
            
            status_counts = {
                "PENDING": 0,
                "APPROVED": 0,
                "REJECTED": 0,
                "EXECUTING": 0,
                "COMPLETED": 0,
                "FAILED": 0
            }
            
            strategy_counts = {}
            
            for tp in tradeplans:
                status = tp.get("status", "UNKNOWN")
                if status in status_counts:
                    status_counts[status] += 1
                
                script_id = tp.get("script_id", "")
                if script_id:
                    if script_id not in strategy_counts:
                        strategy_counts[script_id] = 0
                    strategy_counts[script_id] += 1
            
            print(f"总数量: {len(tradeplans)}")
            print(f"\n按状态分布:")
            for status, count in status_counts.items():
                print(f"  {status:15s}: {count:3d} 个")
            
            print(f"\n按策略分布:")
            for script_id, count in strategy_counts.items():
                print(f"  {script_id}: {count:3d} 个")
            
            print("\n详细信息:")
            for tp in tradeplans:
                print(f"  - [{tp.get('status', 'UNKNOWN'):10s}] {tp.get('name', 'N/A')}")
                print(f"      代码: {tp.get('symbol', 'N/A')} | 收盘: {tp.get('ohlcv', {}).get('close', 0):.2f}")
                print(f"      策略ID: {tp.get('script_id', 'N/A')}")
            
            print("=" * 80)
        else:
            print(f"获取交易计划失败: {response.status_code}")
    except Exception as e:
        print(f"显示统计摘要失败: {e}")


def main():
    """主函数"""
    print("=" * 80)
    print("交易计划测试数据生成工具（通过API）")
    print("包含真实市场数据（OHLCV、涨跌幅等）并关联TradeScript策略")
    print("=" * 80)
    
    # 检查服务器连接
    try:
        response = requests.get(f"{API_BASE_URL}/health")
        if response.status_code != 200:
            print("\n错误: 无法连接到服务器，请确保服务器正在运行")
            return
    except Exception as e:
        print(f"\n错误: 无法连接到服务器: {e}")
        print("请先启动服务器: python run_server.py")
        return
    
    # 询问是否清除现有数据
    print("\n是否清除现有交易计划数据? (y/n): ", end="")
    choice = input().strip().lower()
    
    if choice == 'y' or choice == 'yes':
        clear_all_tradeplans()
    
    # 生成测试数据
    created_count = generate_tradeplan_test_data()
    
    if created_count > 0:
        # 显示统计摘要
        display_tradeplan_summary()
    else:
        print("\n警告: 没有成功创建任何交易计划数据")


if __name__ == "__main__":
    main()
