"""
APK模块的业务逻辑服务
提供APK管理和加固检测功能
"""

import time
from typing import List, Optional, Dict, Any
from peewee import DoesNotExist

from core.apk.database import ApkDatabase
from core.database.models import Apk
from core.apk.models import ApkCreateRequest, ApkUpdateRequest, ApkListResponse, ApkSearchRequest, PackerDetectionRequest, PackerDetectionResult
from core.apk.apk_packer_detector import APKPackerDetector


class ApkManager:
    def __init__(self):
        """初始化APK管理器，使用统一的数据库接口"""
        self.db = ApkDatabase()
        self.packer_detector = APKPackerDetector()
    
    def register_apk(self, request: ApkCreateRequest) -> Apk:
        """注册APK信息"""
        apk_data = request.dict()
        apk = self.db.register_apk(apk_data)
        
        if apk:
            return apk
        
        raise ValueError("APK注册失败")
    
    def get_apk(self, package_name: str) -> Optional[Apk]:
        """获取APK信息"""
        apk = self.db.get_apk(package_name)
        if apk:
            return apk
        return None
    
    def get_all_apks(self) -> List[Apk]:
        """获取所有APK列表"""
        apks = self.db.get_all_apks()
        return apks
    
    def search_apks(self, request: ApkSearchRequest) -> ApkListResponse:
        """搜索APK"""
        search_params = {
            'id': request.id,
            'app_name': request.app_name,
            'version_name': request.version_name,
            'is_packed': request.is_packed,
            'packer_type': request.packer_type,
            'limit': request.limit,
            'offset': request.offset
        }
        
        # 移除None值
        search_params = {k: v for k, v in search_params.items() if v is not None}
        
        apks = self.db.search_apks(**search_params)
        total_count = self.db.get_apk_count()
        
        return ApkListResponse(
            apks=apks,
            total_count=total_count
        )
    
    def update_apk(self, package_name: str, request: ApkUpdateRequest) -> Optional[Apk]:
        """更新APK信息"""
        update_data = request.dict(exclude_unset=True)
        success = self.db.update_apk(package_name, update_data)
        
        if success:
            return self.get_apk(package_name)
        return None
    
    def delete_apk(self, package_name: str) -> bool:
        """删除APK"""
        return self.db.delete_apk(package_name)
    
    def detect_packer(self, request: PackerDetectionRequest) -> PackerDetectionResult:
        """检测APK加固情况"""
        try:
            if request.apk_path:
                # 从文件路径检测
                detection_result = self.packer_detector.detect_packer(request.apk_path)
            elif request.package_name:
                # 从设备检测
                detection_result = self.packer_detector.detect_packer_from_device(
                    request.package_name, request.device_id
                )
            else:
                return PackerDetectionResult(
                    is_packed=False,
                    confidence=0.0,
                    indicators=[],
                    detailed_analysis={},
                    error="必须提供apk_path或package_name参数"
                )
            
            # 如果检测成功且包含包名，保存结果到数据库
            if 'package_name' in detection_result and detection_result.get('package_name'):
                self.db.save_packer_detection_result(
                    detection_result['package_name'], detection_result
                )
            
            return PackerDetectionResult(
                is_packed=detection_result.get('is_packed', False),
                packer_type=detection_result.get('packer_type'),
                confidence=detection_result.get('confidence', 0.0),
                indicators=detection_result.get('indicators', []),
                detailed_analysis=detection_result.get('detailed_analysis', {}),
                error=detection_result.get('error')
            )
            
        except Exception as e:
            return PackerDetectionResult(
                is_packed=False,
                confidence=0.0,
                indicators=[],
                detailed_analysis={},
                error=f"加固检测失败: {str(e)}"
            )
    
    def get_packed_apks(self) -> List[Apk]:
        """获取所有被加固的APK"""
        apks = self.db.get_packed_apks()
        return apks
    
    def get_apks_by_packer_type(self, packer_type: str) -> List[Apk]:
        """按加固类型获取APK"""
        apks = self.db.get_apks_by_packer_type(packer_type)
        return apks
    
    def batch_detect_packers(self, package_names: List[str], device_id: Optional[str] = None) -> List[PackerDetectionResult]:
        """批量检测APK加固情况"""
        results = []
        for package_name in package_names:
            request = PackerDetectionRequest(
                package_name=package_name,
                device_id=device_id
            )
            result = self.detect_packer(request)
            results.append(result)
        
        return results


if __name__ == "__main__":
    """Main entry point for APK manager service"""
    import logging
    import time
    
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    apk_manager = ApkManager()
    logger.info("APK Manager Service Started")
    
    # 测试功能
    try:
        # 测试获取所有APK
        apks = apk_manager.get_all_apks()
        logger.info(f"当前数据库中有 {len(apks)} 个APK记录")
        
        # 测试加固检测
        if apks:
            test_package = apks[0].package_name
            detection_request = PackerDetectionRequest(package_name=test_package)
            result = apk_manager.detect_packer(detection_request)
            logger.info(f"加固检测结果: {result}")
        
    except Exception as e:
        logger.error(f"测试失败: {str(e)}")
    
    # Keep the service running
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        logger.info("APK Manager Service Stopped")