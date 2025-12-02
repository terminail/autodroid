"""
测试基于邮箱地址的简化用户ID生成算法
"""
import pytest
from core.auth.database import UserDatabase

class TestUserIdGeneration:
    """测试用户ID生成算法"""
    
    def setup_method(self):
        """测试前初始化"""
        self.db = UserDatabase()
    
    def test_basic_email_id_generation(self):
        """测试基本邮箱ID生成"""
        # 测试简单邮箱地址
        email = "test@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"
        
        # 测试包含数字的邮箱
        email = "user123@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "user123"
    
    def test_email_with_special_characters(self):
        """测试包含特殊字符的邮箱"""
        # 测试包含点号的邮箱
        email = "test.user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
        
        # 测试包含下划线的邮箱
        email = "test_user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
        
        # 测试包含连字符的邮箱
        email = "test-user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"
    
    def test_email_with_multiple_special_chars(self):
        """测试包含多个特殊字符的邮箱"""
        email = "test.user-name_123@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testusername123"
    
    def test_short_email_prefix(self):
        """测试邮箱前缀过短的情况"""
        # 测试单字符前缀
        email = "a@example.com"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3  # 应该包含随机后缀
        assert user_id.startswith("a")
        
        # 测试双字符前缀
        email = "ab@example.com"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3  # 应该包含随机后缀
        assert user_id.startswith("ab")
    
    def test_case_insensitive(self):
        """测试大小写不敏感"""
        email_upper = "TEST@example.com"
        email_lower = "test@example.com"
        
        user_id_upper = self.db._generate_user_id(email_upper)
        user_id_lower = self.db._generate_user_id(email_lower)
        
        assert user_id_upper == user_id_lower == "test"
    
    def test_international_email(self):
        """测试国际化邮箱地址"""
        # 测试包含非ASCII字符的邮箱（非ASCII字符会被过滤）
        email = "用户@example.com"
        user_id = self.db._generate_user_id(email)
        # 非ASCII字符被过滤后，如果只剩空字符串，会生成随机ID
        assert len(user_id) >= 3  # 应该生成随机ID
        
        # 测试混合字符的邮箱
        email = "user123@例子.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "user123"
    
    def test_empty_and_invalid_emails(self):
        """测试空和无效邮箱地址"""
        # 测试空邮箱
        email = ""
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3  # 应该生成随机ID
        
        # 测试无效格式邮箱
        email = "invalid-email"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3  # 应该生成随机ID
    
    def test_user_id_uniqueness(self):
        """测试用户ID唯一性处理"""
        # 模拟重复用户ID的情况
        email1 = "test@example.com"
        email2 = "test@example.org"
        
        # 两个邮箱应该生成相同的用户ID基础
        user_id_base1 = self.db._generate_user_id(email1)
        user_id_base2 = self.db._generate_user_id(email2)
        
        # 在数据库层面，create_user方法应该处理重复ID
        # 这里我们测试生成的基础ID是否相同
        assert user_id_base1 == user_id_base2 == "test"

class TestUserDatabaseOperations:
    """测试用户数据库操作"""
    
    def setup_method(self):
        """测试前初始化"""
        import os
        import uuid
        # 使用唯一数据库文件名，避免测试间冲突
        self.test_db_path = f"test_users_{uuid.uuid4().hex[:8]}.db"
        
        # 确保数据库文件被完全清理
        self._force_cleanup_database()
        
        self.db = UserDatabase(self.test_db_path)
    
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
    
    def test_user_creation_with_generated_id(self):
        """测试使用生成ID创建用户"""
        email = "test@example.com"
        password = "password123"
        name = "Test User"
        
        user_id = self.db.create_user(email, name, password)
        assert user_id is not None
        assert user_id == "test"  # 应该使用基于邮箱的简化ID
        
        # 验证用户信息
        user_info = self.db.get_user_by_id(user_id)
        assert user_info["email"] == email
        assert user_info["name"] == name
    
    def test_user_id_collision_handling(self):
        """测试用户ID冲突处理"""
        # 创建第一个用户
        email1 = "test@example.com"
        user_id1 = self.db.create_user(email1, "User One", "pass1")
        assert user_id1 == "test"
        
        # 创建第二个用户，邮箱不同但ID基础相同
        email2 = "test@example.org"
        user_id2 = self.db.create_user(email2, "User Two", "pass2")
        
        # 第二个用户应该获得不同的ID（添加数字后缀）
        assert user_id2 != user_id1
        assert user_id2.startswith("test")
        assert user_id2.endswith("1")  # 应该是test1
    
    def test_get_user_id_by_email(self):
        """测试根据邮箱获取用户ID"""
        email = "test@example.com"
        expected_user_id = "test"
        
        # 创建用户
        self.db.create_user(email, "Test User", "password")
        
        # 根据邮箱获取用户ID
        user_id = self.db.get_user_id_by_email(email)
        assert user_id == expected_user_id
    
    def test_get_user_by_email(self):
        """测试根据邮箱获取用户信息"""
        email = "test@example.com"
        name = "Test User"
        
        # 创建用户
        self.db.create_user(email, name, "password")
        
        # 根据邮箱获取用户信息
        user_info = self.db.get_user_by_email(email)
        assert user_info["email"] == email
        assert user_info["name"] == name
        assert user_info["id"] == "test"
    
    def test_duplicate_email_prevention(self):
        """测试重复邮箱注册预防"""
        email = "test@example.com"
        
        # 第一次注册应该成功
        user_id1 = self.db.create_user(email, "User One", "pass1")
        assert user_id1 is not None
        
        # 第二次注册相同邮箱应该失败
        user_id2 = self.db.create_user(email, "User Two", "pass2")
        assert user_id2 is None

if __name__ == "__main__":
    # 运行测试
    pytest.main([__file__, "-v"])