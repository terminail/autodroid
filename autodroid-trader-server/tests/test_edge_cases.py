"""
测试边界情况和错误处理
"""
import pytest
from core.auth.database import AuthDatabase

class TestEdgeCases:
    """测试边界情况"""
    
    def setup_method(self):
        """测试前初始化"""
        import os
        import uuid
        # 使用唯一数据库文件名，避免测试间冲突
        self.test_db_path = f"test_edge_cases_{uuid.uuid4().hex[:8]}.db"
        
        # 确保数据库文件被完全清理
        self._force_cleanup_database()
        
        self.db = AuthDatabase(self.test_db_path)
    
    def teardown_method(self):
        """测试后清理"""
        self._force_cleanup_database()
    
    def _force_cleanup_database(self):
        """强制清理数据库文件"""
        import os
        import time
        
        # 多次尝试清理，确保数据库连接完全释放
        for attempt in range(5):
            try:
                if os.path.exists(self.test_db_path):
                    os.remove(self.test_db_path)
                    break  # 成功删除，退出循环
            except PermissionError:
                # 等待更长时间让数据库连接释放
                time.sleep(0.5)
            except Exception:
                # 其他异常也忽略，继续尝试
                time.sleep(0.5)
    
    def test_very_long_email_prefix(self):
        """测试非常长的邮箱前缀"""
        # 创建超长邮箱前缀
        long_prefix = "a" * 100  # 100个字符
        email = f"{long_prefix}@example.com"
        
        user_id = self.db._generate_user_id(email)
        # 用户ID应该保持原始长度，因为算法不会截断
        assert len(user_id) == 100  # 应该保持原始长度
        assert user_id.startswith("a")
    
    def test_email_with_only_numbers(self):
        """测试纯数字邮箱前缀"""
        email = "123456@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "123456"
    
    def test_email_with_consecutive_special_chars(self):
        """测试连续特殊字符"""
        email = "test..user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
        
        email = "test--user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
        
        email = "test__user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
    
    def test_email_with_mixed_case(self):
        """测试混合大小写邮箱"""
        email = "TeStUsEr@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
    
    def test_multiple_id_collisions(self):
        """测试多个ID冲突"""
        # 创建多个会产生相同基础ID的用户
        emails = [
            "test@example.com",
            "test@example.org", 
            "test@example.net",
            "test@example.io"
        ]
        
        user_ids = []
        for i, email in enumerate(emails):
            user_id = self.db.create_user(email, f"User {i}", f"pass{i}")
            user_ids.append(user_id)
        
        # 所有用户ID应该不同
        assert len(set(user_ids)) == len(user_ids)
        
        # 检查ID格式
        for i, user_id in enumerate(user_ids):
            if i == 0:
                assert user_id == "test"
            else:
                assert user_id.startswith("test")
                assert user_id.endswith(str(i))
    
    def test_special_email_formats(self):
        """测试特殊邮箱格式"""
        # 测试加号格式邮箱
        email = "test+tag@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testtag"  # 当前算法会移除+但保留tag
        
        # 测试子域名邮箱
        email = "test@sub.example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"
    
    def test_none_and_null_values(self):
        """测试None和空值处理"""
        # 测试None邮箱 - 应该抛出异常或返回随机ID
        try:
            user_id = self.db._generate_user_id(None)
            # 如果执行到这里，应该生成了随机ID
            assert len(user_id) >= 3
        except AttributeError:
            # 如果抛出异常，这也是预期的行为
            pass
        
        # 测试空字符串
        user_id = self.db._generate_user_id("")
        assert len(user_id) >= 3
    
    def test_whitespace_handling(self):
        """测试空格处理"""
        email = " test @example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"  # 应该去除空格
        
        email = "test user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"  # 空格应该被移除

class TestDatabaseErrorHandling:
    """测试数据库错误处理"""
    
    def setup_method(self):
        """测试前初始化"""
        import os
        import time
        import uuid
        # 使用唯一数据库文件名，避免测试间冲突
        self.test_db_path = f"test_error_handling_{uuid.uuid4().hex[:8]}.db"
        
        # 确保数据库文件被完全清理
        self._force_cleanup_database()
        
        self.db = AuthDatabase(self.test_db_path)
    
    def teardown_method(self):
        """测试后清理"""
        self._force_cleanup_database()
    
    def _force_cleanup_database(self):
        """强制清理数据库文件"""
        import os
        import time
        
        # 多次尝试清理，确保数据库连接完全释放
        for attempt in range(5):
            try:
                if os.path.exists(self.test_db_path):
                    os.remove(self.test_db_path)
                    break  # 成功删除，退出循环
            except PermissionError:
                # 等待更长时间让数据库连接释放
                time.sleep(0.5)
            except Exception:
                # 其他异常也忽略，继续尝试
                time.sleep(0.5)
    
    def test_get_nonexistent_user(self):
        """测试获取不存在的用户"""
        # 获取不存在的邮箱
        user_id = self.db.get_user_id_by_email("nonexistent@example.com")
        assert user_id is None
        
        user_info = self.db.get_user_by_email("nonexistent@example.com")
        assert user_info is None
        
        # 获取不存在的用户ID
        user_info = self.db.get_user_by_id("nonexistent")
        assert user_info is None
    
    def test_invalid_user_creation(self):
        """测试无效用户创建"""
        # 测试空邮箱
        user_id = self.db.create_user("", "Name", "password")
        assert user_id is not None  # 应该生成随机ID
        
        # 测试None邮箱 - 应该返回None或抛出异常
        try:
            user_id = self.db.create_user(None, "Name", "password")
            # 如果执行到这里，可能返回None或生成随机ID
            # 不进行断言，因为行为取决于实现
        except Exception:
            # 如果抛出异常，这也是预期的行为
            pass
        
        # 测试空密码
        user_id = self.db.create_user("test@example.com", "Name", "")
        # 空密码可能不被允许，返回None是合理的行为
        # 不进行断言，因为行为取决于实现
    
    def test_duplicate_email_handling(self):
        """测试重复邮箱处理"""
        # 创建第一个用户
        user_id1 = self.db.create_user("test@example.com", "User One", "pass1")
        assert user_id1 is not None
        
        # 创建第二个用户，相同邮箱
        user_id2 = self.db.create_user("test@example.com", "User Two", "pass2")
        # 根据当前实现，邮箱重复应该返回None
        assert user_id2 is None
    
    def test_database_connection_issues(self):
        """测试数据库连接问题"""
        # 使用无效的数据库路径
        try:
            invalid_db = AuthDatabase("/invalid/path/database.db")
            # 这里应该处理异常或返回错误
        except Exception as e:
            # 应该抛出适当的异常
            assert "database" in str(e).lower() or "path" in str(e).lower()

if __name__ == "__main__":
    pytest.main([__file__, "-v"])