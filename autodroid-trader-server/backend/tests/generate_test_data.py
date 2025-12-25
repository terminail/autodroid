import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

import json
import uuid
from datetime import datetime
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def generate_tradescript_id() -> str:
    return f"ts_{uuid.uuid4().hex[:16]}"


def generate_tradeplan_id() -> str:
    return f"tp_{uuid.uuid4().hex[:16]}"


TRADESCRIPT_TEMPLATES = [
    {
        "name": "网格交易策略",
        "description": "使用网格交易策略在目标股票上进行交易",
        "metadata": {
            "strategy_type": "grid_trading",
            "parameters": {
                "grid_size": 0.5,
                "grid_count": 10.0,
                "amount_per_grid": 10000.0,
                "upper_bound": 420.0,
                "lower_bound": 380.0
            }
        }
    },
    {
        "name": "RSI反转策略",
        "description": "基于RSI指标的反转交易策略",
        "metadata": {
            "strategy_type": "rsi_reversal",
            "parameters": {
                "rsi_oversold": 30.0,
                "rsi_overbought": 70.0,
                "rsi_period": 14.0,
                "position_size": 15000.0,
                "stop_loss": 5.0
            }
        }
    },
    {
        "name": "均线突破策略",
        "description": "当价格突破均线时执行买入操作",
        "metadata": {
            "strategy_type": "ma_breakout",
            "parameters": {
                "ma_period": 20.0,
                "ma_type": "SMA",
                "position_size": 20000.0,
                "breakout_threshold": 0.5,
                "trailing_stop": 3.0
            }
        }
    },
    {
        "name": "定期定额投资策略",
        "description": "定期定额投资目标股票",
        "metadata": {
            "strategy_type": "dca",
            "parameters": {
                "amount": 5000.0,
                "frequency": "monthly",
                "investment_day": 1.0,
                "auto_reinvest": True
            }
        }
    },
    {
        "name": "布林带策略",
        "description": "价格突破布林带上轨时买入",
        "metadata": {
            "strategy_type": "bollinger_bands",
            "parameters": {
                "period": 20.0,
                "std_dev": 2.0,
                "position_size": 18000.0,
                "take_profit": 3.0,
                "stop_loss": 2.0
            }
        }
    },
    {
        "name": "MACD趋势策略",
        "description": "基于MACD指标的趋势跟踪策略",
        "metadata": {
            "strategy_type": "macd_trend",
            "parameters": {
                "fast_period": 12.0,
                "slow_period": 26.0,
                "signal_period": 9.0,
                "position_size": 25000.0,
                "trailing_stop": 4.0
            }
        }
    }
]

STOCK_DATA = [
    {
        "exchange": "HKEX",
        "symbol": "00700.HK",
        "symbol_name": "腾讯控股",
        "ohlcv": {
            "close": 402.3,
            "high": 405.2,
            "low": 395.8,
            "open": 398.5,
            "volume": 15800000.0
        },
        "change_percent": 1.25
    },
    {
        "exchange": "HKEX",
        "symbol": "00941.HK",
        "symbol_name": "中国移动",
        "ohlcv": {
            "close": 72.9,
            "high": 73.2,
            "low": 71.8,
            "open": 72.5,
            "volume": 8500000.0
        },
        "change_percent": 0.55
    },
    {
        "exchange": "HKEX",
        "symbol": "01299.HK",
        "symbol_name": "友邦保险",
        "ohlcv": {
            "close": 68.9,
            "high": 69.5,
            "low": 67.8,
            "open": 68.2,
            "volume": 12000000.0
        },
        "change_percent": 1.02
    },
    {
        "exchange": "HKEX",
        "symbol": "03690.HK",
        "symbol_name": "美团",
        "ohlcv": {
            "close": 126.5,
            "high": 128.5,
            "low": 124.2,
            "open": 125.8,
            "volume": 25000000.0
        },
        "change_percent": 0.56
    }
]


def get_supported_apps() -> list:
    """从配置文件获取支持的APK列表"""
    import yaml
    config_path = os.path.join(os.path.dirname(__file__), '..', 'config.yaml')
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        apps = config.get('supported_apps', [])
        return [
            {
                "id": f"apk_{i+1:03d}",
                "package_name": app["app_package"],
                "app_name": app["name"],
                "name": app["name"],
                "description": f"Supported app: {app['name']}",
                "version": "1.0.0",
                "version_code": 1
            }
            for i, app in enumerate(apps)
        ]
    except Exception as e:
        logger.warning(f"Failed to read config.yaml: {e}, using default apps")
        return [
            {
                "id": "apk_001",
                "package_name": "com.autodroid.trader",
                "app_name": "自动交易花",
                "name": "Autodroid Trader",
                "description": "Main trading application",
                "version": "1.0.0",
                "version_code": 1
            },
            {
                "id": "apk_002",
                "package_name": "com.tdx.androidCCZQ",
                "app_name": "明佣宝",
                "name": "MingYongBao",
                "description": "Commission rebate app",
                "version": "1.0.0",
                "version_code": 1
            }
        ]


def create_demo_apks() -> list:
    """创建演示用APK数据"""
    from core.database.models import Apk
    
    demo_apks = get_supported_apps()
    
    created_apks = []
    for apk_data in demo_apks:
        try:
            apk, created = Apk.get_or_create(
                package_name=apk_data["package_name"],
                defaults=apk_data
            )
            if created:
                logger.info(f"Created APK: {apk.package_name}")
            created_apks.append(apk)
        except Exception as e:
            logger.error(f"Failed to create APK {apk_data['package_name']}: {e}")
    
    return created_apks


def create_demo_tradescripts(tradescript_db, apks: list, count: int = None) -> list:
    """创建演示用交易脚本数据"""
    from core.database.models import TradeScript
    
    if count is None:
        count = len(TRADESCRIPT_TEMPLATES)
    
    created_scripts = []
    for i, template in enumerate(TRADESCRIPT_TEMPLATES[:count]):
        script_id = generate_tradescript_id()
        apk = apks[i % len(apks)] if apks else None
        
        try:
            if apk:
                TradeScript.create(
                    id=script_id,
                    apk=apk,
                    name=template["name"],
                    description=template["description"],
                    metadata=json.dumps(template["metadata"]),
                    script_path=f"/scripts/{template['metadata']['strategy_type']}.py",
                    status="ACTIVE"
                )
            else:
                logger.warning(f"No APK available for tradescript {template['name']}, skipping")
                continue
            
            created_scripts.append({
                "id": script_id,
                "name": template["name"],
                "description": template["description"],
                "metadata": template["metadata"]
            })
            logger.info(f"Created tradescript: {script_id} - {template['name']}")
        except Exception as e:
            logger.error(f"Failed to create tradescript {template['name']}: {e}")
    
    return created_scripts


def create_demo_tradeplans(tradeplan_db, tradescripts: list, create_two_per_combo: bool = True) -> int:
    """创建演示用交易计划数据"""
    from core.database.models import TradePlan
    
    created_count = 0
    
    for stock in STOCK_DATA:
        for script in tradescripts:
            num_plans = 2 if create_two_per_combo else 1
            for j in range(num_plans):
                tradeplan_id = generate_tradeplan_id()
                now = datetime.utcnow()
                
                try:
                    TradePlan.create(
                        id=tradeplan_id,
                        script_id=script["id"],
                        name=f"{script['name']} - {stock['symbol_name']}",
                        description=script["description"],
                        exchange=stock["exchange"],
                        symbol=stock["symbol"],
                        symbol_name=stock["symbol_name"],
                        ohlcv=json.dumps(stock["ohlcv"]),
                        change_percent=stock["change_percent"],
                        data=json.dumps(script["metadata"]["parameters"]),
                        status="REJECTED",
                        createdAt=now,
                        updatedAt=now
                    )
                    created_count += 1
                    logger.info(f"Created tradeplan: {tradeplan_id} - {script['name']} - {stock['symbol_name']}")
                except Exception as e:
                    logger.error(f"Failed to create tradeplan for {stock['symbol_name']} with {script['name']}: {e}")
    
    return created_count


def run():
    try:
        from core.tradescript.database import TradeScriptDatabase
        from core.tradeplan.database import TradePlanDatabase
    except ImportError as e:
        logger.error(f"Failed to import database modules: {e}")
        logger.info("Trying with backend prefix...")
        from backend.core.tradescript.database import TradeScriptDatabase
        from backend.core.tradeplan.database import TradePlanDatabase
    
    logger.info("Starting test data generation...")
    
    logger.info("Creating APKs...")
    created_apks = create_demo_apks()
    logger.info(f"Created {len(created_apks)} APKs")
    
    logger.info("Creating tradescripts...")
    tradescript_db = TradeScriptDatabase()
    created_scripts = create_demo_tradescripts(tradescript_db, created_apks)
    
    if not created_scripts:
        logger.warning("No tradescripts created, checking existing scripts...")
        all_scripts = tradescript_db.get_all_tradescripts()
        if all_scripts:
            created_scripts = [
                {
                    "id": s["id"],
                    "name": s["name"],
                    "metadata": json.loads(s.get("metadata", "{}"))
                }
                for s in all_scripts
            ]
            logger.info(f"Using {len(created_scripts)} existing tradescripts")
        else:
            logger.error("No tradescripts available and could not create new ones")
            return
    
    logger.info(f"Created {len(created_scripts)} tradescripts")
    
    logger.info("Creating tradeplans...")
    tradeplan_db = TradePlanDatabase()
    created_tradeplans = create_demo_tradeplans(tradeplan_db, created_scripts)
    
    logger.info(f"Created {created_tradeplans} tradeplans")
    
    logger.info("Test data generation completed!")
    logger.info(f"Summary: {len(created_scripts)} tradescripts, {created_tradeplans} tradeplans")


if __name__ == "__main__":
    run()
