"""
分析模块数据库服务类
"""

import json
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist, fn

from core.database.base import BaseDatabase
from core.database.models import AnalysisResult, AnalysisTask, AnalysisPattern, AnalysisReport


class AnalysisDatabase(BaseDatabase):
    """分析模块数据库服务类"""
    
    def __init__(self):
        """初始化分析数据库服务"""
        super().__init__()
    
    def create_analysis_result(self, apk_id: str, analysis_data: Dict[str, Any]) -> Optional[AnalysisResult]:
        """创建分析结果记录"""
        try:
            with AnalysisResult._meta.database.atomic():
                result = AnalysisResult.create(
                    id=analysis_data.get('id', f"result_{apk_id}_{analysis_data.get('analysis_type', 'general')}"),
                    apk=apk_id,
                    analysis_type=analysis_data.get('analysis_type', 'general'),
                    result=json.dumps(analysis_data.get('result', {}), ensure_ascii=False),
                    confidence=analysis_data.get('confidence', 0.0),
                    analysis_time=analysis_data.get('analysis_time')
                )
                return result
        except Exception as e:
            print(f"创建分析结果失败: {str(e)}")
            return None
    
    def get_analysis_result(self, result_id: str) -> Optional[AnalysisResult]:
        """获取特定分析结果"""
        try:
            return AnalysisResult.get(AnalysisResult.id == result_id)
        except DoesNotExist:
            return None
    
    def get_analysis_results_by_apk(self, apk_id: str, limit: int = 100) -> List[AnalysisResult]:
        """获取特定APK的分析结果"""
        try:
            return list(AnalysisResult
                       .select()
                       .where(AnalysisResult.apk == apk_id)
                       .order_by(AnalysisResult.analysis_time.desc())
                       .limit(limit))
        except Exception as e:
            print(f"获取分析结果失败: {str(e)}")
            return []
    
    def get_latest_analysis_result(self, apk_id: str) -> Optional[AnalysisResult]:
        """获取特定APK的最新分析结果"""
        try:
            return (AnalysisResult
                    .select()
                    .where(AnalysisResult.apk == apk_id)
                    .order_by(AnalysisResult.analysis_time.desc())
                    .first())
        except Exception as e:
            print(f"获取最新分析结果失败: {str(e)}")
            return None
    
    def update_analysis_result(self, result_id: str, update_data: Dict[str, Any]) -> bool:
        """更新分析结果"""
        try:
            valid_fields = {"analysis_type", "result", "confidence", "analysis_time"}
            update_fields = {}
            
            for field in valid_fields:
                if field in update_data and update_data[field] is not None:
                    if field == 'result':
                        update_fields[field] = json.dumps(update_data[field], ensure_ascii=False)
                    else:
                        update_fields[field] = update_data[field]
            
            if not update_fields:
                return False
            
            query = AnalysisResult.update(**update_fields).where(AnalysisResult.id == result_id)
            return query.execute() > 0
            
        except Exception as e:
            print(f"更新分析结果失败: {str(e)}")
            return False
    
    def delete_analysis_result(self, result_id: str) -> bool:
        """删除分析结果"""
        try:
            deleted_count = AnalysisResult.delete().where(AnalysisResult.id == result_id).execute()
            return deleted_count > 0
        except Exception as e:
            print(f"删除分析结果失败: {str(e)}")
            return False
    
    def get_analysis_statistics(self, apk_id: str) -> Dict[str, Any]:
        """获取分析统计信息"""
        try:
            total_results = AnalysisResult.select().where(AnalysisResult.apk == apk_id).count()
            
            # 按分析类型统计
            type_counts = {}
            for result in AnalysisResult.select(AnalysisResult.analysis_type).where(AnalysisResult.apk == apk_id):
                analysis_type = result.analysis_type
                type_counts[analysis_type] = type_counts.get(analysis_type, 0) + 1
            
            # 获取平均置信度
            avg_confidence = (AnalysisResult
                            .select(fn.AVG(AnalysisResult.confidence))
                            .where(AnalysisResult.apk == apk_id)
                            .scalar() or 0.0)
            
            return {
                'total_results': total_results,
                'analysis_type_counts': type_counts,
                'average_confidence': round(avg_confidence, 2)
            }
        except Exception as e:
            print(f"获取分析统计失败: {str(e)}")
            return {'total_results': 0, 'analysis_type_counts': {}, 'average_confidence': 0.0}
    
    def search_analysis_results(self, **kwargs) -> List[AnalysisResult]:
        """搜索分析结果"""
        try:
            query = AnalysisResult.select()
            
            if kwargs.get('apk_id'):
                query = query.where(AnalysisResult.apk == kwargs['apk_id'])
            
            if kwargs.get('analysis_type'):
                query = query.where(AnalysisResult.analysis_type.contains(kwargs['analysis_type']))
            
            if kwargs.get('min_confidence') is not None:
                query = query.where(AnalysisResult.confidence >= kwargs['min_confidence'])
            
            if kwargs.get('max_confidence') is not None:
                query = query.where(AnalysisResult.confidence <= kwargs['max_confidence'])
            
            # 时间范围查询
            if kwargs.get('start_time'):
                query = query.where(AnalysisResult.analysis_time >= kwargs['start_time'])
            
            if kwargs.get('end_time'):
                query = query.where(AnalysisResult.analysis_time <= kwargs['end_time'])
            
            # 应用分页
            limit = kwargs.get('limit', 100)
            offset = kwargs.get('offset', 0)
            query = query.limit(limit).offset(offset)
            
            return list(query.order_by(AnalysisResult.analysis_time.desc()))
            
        except Exception as e:
            print(f"搜索分析结果失败: {str(e)}")
            return []