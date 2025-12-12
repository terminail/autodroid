"""
用户操作管理服务类 - 按照server-database-model模式实现
"""

import json
from typing import Dict, List, Optional, Any

from .database import UserOperationDatabase
from core.database.models import UserOperation


class UserOperationManager:
    """用户操作管理服务类"""
    
    def __init__(self):
        """初始化用户操作管理服务"""
        self.db = UserOperationDatabase()
    
    def save_operation(self, operation_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """保存用户操作记录"""
        operation = self.db.save_operation(operation_data)
        if operation:
            return self._operation_to_dict(operation)
        return None
    
    def get_operation(self, operation_id: str) -> Optional[Dict[str, Any]]:
        """获取特定用户操作记录"""
        operation = self.db.get_operation(operation_id)
        if operation:
            return self._operation_to_dict(operation)
        return None
    
    def get_operations_by_apk(self, apk_id: str, limit: int = 100) -> List[Dict[str, Any]]:
        """获取特定APK的用户操作列表"""
        operations = self.db.get_operations_by_apk(apk_id, limit)
        return [self._operation_to_dict(operation) for operation in operations]
    
    def get_recent_operations(self, limit: int = 50) -> List[Dict[str, Any]]:
        """获取最近的操作记录"""
        operations = self.db.get_recent_operations(limit)
        return [self._operation_to_dict(operation) for operation in operations]
    
    def update_operation(self, operation_id: str, update_data: Dict[str, Any]) -> bool:
        """更新用户操作记录"""
        return self.db.update_operation(operation_id, update_data)
    
    def delete_operation(self, operation_id: str) -> bool:
        """删除用户操作记录"""
        return self.db.delete_operation(operation_id)
    
    def search_operations(self, **kwargs) -> List[Dict[str, Any]]:
        """搜索用户操作"""
        operations = self.db.search_operations(**kwargs)
        return [self._operation_to_dict(operation) for operation in operations]
    
    def get_operation_count(self) -> int:
        """获取操作总数"""
        return self.db.get_operation_count()
    
    def get_operations_by_type(self, operation_type: str, limit: int = 50) -> List[Dict[str, Any]]:
        """按类型获取用户操作"""
        operations = self.db.get_operations_by_type(operation_type, limit)
        return [self._operation_to_dict(operation) for operation in operations]
    
    def get_operations_statistics(self, apk_id: Optional[str] = None) -> Dict[str, Any]:
        """获取操作统计信息"""
        return self.db.get_operations_statistics(apk_id)
    
    def associate_operation_with_screenshot(self, operation_id: str, screenshot_id: str) -> bool:
        """关联操作与截屏"""
        return self.db.associate_operation_with_screenshot(operation_id, screenshot_id)
    
    def get_operations_by_screenshot(self, screenshot_id: str) -> List[Dict[str, Any]]:
        """获取特定截屏关联的操作"""
        operations = self.db.get_operations_by_screenshot(screenshot_id)
        return [self._operation_to_dict(operation) for operation in operations]
    
    def record_operation_sequence(self, sequence_data: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """记录操作序列"""
        try:
            saved_operations = []
            
            for operation_data in sequence_data:
                operation = self.save_operation(operation_data)
                if operation:
                    saved_operations.append(operation)
            
            return saved_operations
            
        except Exception as e:
            print(f"记录操作序列失败: {e}")
            return []
    
    def analyze_operation_patterns(self, apk_id: Optional[str] = None) -> Dict[str, Any]:
        """分析操作模式"""
        try:
            # 获取相关操作数据
            if apk_id:
                operations = self.get_operations_by_apk(apk_id, limit=1000)
            else:
                operations = self.get_recent_operations(limit=1000)
            
            if not operations:
                return {"error": "没有操作数据可分析"}
            
            # 分析操作类型分布
            type_distribution = {}
            for operation in operations:
                op_type = operation.get('operation_type', 'unknown')
                type_distribution[op_type] = type_distribution.get(op_type, 0) + 1
            
            # 分析常用操作序列
            common_sequences = self._find_common_sequences(operations)
            
            # 分析操作时间分布
            time_distribution = self._analyze_time_distribution(operations)
            
            return {
                "total_operations": len(operations),
                "type_distribution": type_distribution,
                "common_sequences": common_sequences,
                "time_distribution": time_distribution,
                "average_operations_per_session": self._calculate_average_operations(operations)
            }
            
        except Exception as e:
            print(f"分析操作模式失败: {e}")
            return {"error": str(e)}
    
    def generate_operation_report(self, apk_id: Optional[str] = None) -> Dict[str, Any]:
        """生成操作报告"""
        try:
            statistics = self.get_operations_statistics(apk_id)
            patterns = self.analyze_operation_patterns(apk_id)
            
            report = {
                "summary": statistics,
                "patterns": patterns,
                "recommendations": self._generate_recommendations(statistics, patterns)
            }
            
            return report
            
        except Exception as e:
            print(f"生成操作报告失败: {e}")
            return {"error": str(e)}
    
    def _operation_to_dict(self, operation: UserOperation) -> Dict[str, Any]:
        """将UserOperation模型转换为字典"""
        return {
            'operation_id': operation.id,
            'apk_id': operation.apk.id if operation.apk else None,
            'screenshot_id': operation.screenshot.id if operation.screenshot else None,
            'operation_type': operation.operation_type,
            'target_element': json.loads(operation.target_element) if operation.target_element else {},
            'input_data': operation.input_data,
            'timestamp': operation.timestamp.isoformat() if operation.timestamp else None,
            'duration': operation.duration,
            'success': operation.success,
            'error_message': operation.error_message,
            'additional_info': json.loads(operation.additional_info) if operation.additional_info else {}
        }
    
    def _find_common_sequences(self, operations: List[Dict[str, Any]], min_length: int = 3) -> List[Dict[str, Any]]:
        """查找常见操作序列"""
        # 简化实现 - 实际应用中可以使用更复杂的算法
        sequences = []
        current_sequence = []
        
        for operation in operations:
            if len(current_sequence) < min_length:
                current_sequence.append(operation['operation_type'])
            else:
                sequences.append(current_sequence.copy())
                current_sequence = [operation['operation_type']]
        
        # 统计序列频率
        sequence_counts = {}
        for seq in sequences:
            seq_key = ' -> '.join(seq)
            sequence_counts[seq_key] = sequence_counts.get(seq_key, 0) + 1
        
        # 返回最常见的序列
        common_sequences = []
        for seq_key, count in sorted(sequence_counts.items(), key=lambda x: x[1], reverse=True)[:5]:
            common_sequences.append({
                'sequence': seq_key,
                'frequency': count,
                'operations': seq_key.split(' -> ')
            })
        
        return common_sequences
    
    def _analyze_time_distribution(self, operations: List[Dict[str, Any]]) -> Dict[str, int]:
        """分析时间分布"""
        time_distribution = {
            'morning': 0,    # 6:00-12:00
            'afternoon': 0,  # 12:00-18:00
            'evening': 0,    # 18:00-24:00
            'night': 0       # 0:00-6:00
        }
        
        for operation in operations:
            if operation.get('timestamp'):
                # 简化实现 - 实际应用中需要解析时间戳
                hour = 12  # 假设中午
                if hour < 6:
                    time_distribution['night'] += 1
                elif hour < 12:
                    time_distribution['morning'] += 1
                elif hour < 18:
                    time_distribution['afternoon'] += 1
                else:
                    time_distribution['evening'] += 1
        
        return time_distribution
    
    def _calculate_average_operations(self, operations: List[Dict[str, Any]]) -> float:
        """计算平均操作数"""
        if not operations:
            return 0.0
        
        # 简化实现 - 实际应用中需要按会话分组
        return len(operations) / 10.0  # 假设10个会话
    
    def _generate_recommendations(self, statistics: Dict[str, Any], patterns: Dict[str, Any]) -> List[str]:
        """生成推荐建议"""
        recommendations = []
        
        # 基于统计数据和模式生成建议
        if statistics.get('total_operations', 0) > 100:
            recommendations.append("操作数据较多，建议进行数据清理和归档")
        
        if patterns.get('type_distribution', {}).get('click', 0) > patterns.get('type_distribution', {}).get('input', 0) * 2:
            recommendations.append("点击操作较多，建议优化界面交互设计")
        
        if patterns.get('average_operations_per_session', 0) > 20:
            recommendations.append("单次会话操作较多，建议简化操作流程")
        
        return recommendations


def main():
    """主函数 - 用于测试"""
    manager = UserOperationManager()
    
    # 测试操作保存
    operation_data = {
        'operation_id': 'test_operation_001',
        'apk_id': 'test_apk_001',
        'operation_type': 'click',
        'target_element': '{"id": "button_login", "text": "登录"}',
        'input_data': '',
        'timestamp': '2024-01-01T10:00:00',
        'duration': 1.5,
        'success': True,
        'error_message': '',
        'additional_info': '{"x": 100, "y": 200}'
    }
    
    operation = manager.save_operation(operation_data)
    if operation:
        print(f"✅ 操作保存成功: {operation}")
    else:
        print("❌ 操作保存失败")


if __name__ == "__main__":
    main()