"""
Authentication tests - combines edge cases and user ID generation tests
"""

import pytest
from core.auth.database import AuthDatabase


class TestEdgeCases:
    """测试边界情况"""

    def setup_method(self):
        """测试前初始化"""
        self.db = AuthDatabase()

    def teardown_method(self):
        """测试后清理"""
        if self.db:
            self.db.close()

    def test_very_long_email_prefix(self):
        """测试非常长的邮箱前缀"""
        long_prefix = "a" * 100
        email = f"{long_prefix}@example.com"

        user_id = self.db._generate_user_id(email)
        assert len(user_id) == 100
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

        assert len(set(user_ids)) == len(user_ids)

        for i, user_id in enumerate(user_ids):
            if i == 0:
                assert user_id == "test"
            else:
                assert user_id.startswith("test")
                assert user_id.endswith(str(i))

    def test_special_email_formats(self):
        """测试特殊邮箱格式"""
        email = "test+tag@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testtag"

        email = "test@sub.example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"

    def test_none_and_null_values(self):
        """测试None和空值处理"""
        try:
            user_id = self.db._generate_user_id(None)
            assert len(user_id) >= 3
        except AttributeError:
            pass

        user_id = self.db._generate_user_id("")
        assert len(user_id) >= 3

    def test_whitespace_handling(self):
        """测试空格处理"""
        email = " test @example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"

        email = "test user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"


class TestDatabaseErrorHandling:
    """测试数据库错误处理"""

    def setup_method(self):
        """测试前初始化"""
        self.db = AuthDatabase()

    def teardown_method(self):
        """测试后清理"""
        if self.db:
            self.db.close()

    def test_get_nonexistent_user(self):
        """测试获取不存在的用户"""
        user_id = self.db.get_user_id_by_email("nonexistent@example.com")
        assert user_id is None

        user_info = self.db.get_user_by_email("nonexistent@example.com")
        assert user_info is None

        user_info = self.db.get_user_by_id("nonexistent")
        assert user_info is None

    def test_invalid_user_creation(self):
        """测试无效用户创建"""
        user_id = self.db.create_user("", "Name", "password")
        assert user_id is not None

        try:
            user_id = self.db.create_user(None, "Name", "password")
        except Exception:
            pass

    def test_duplicate_email_handling(self):
        """测试重复邮箱处理"""
        user_id1 = self.db.create_user("test@example.com", "User One", "pass1")
        assert user_id1 is not None

        user_id2 = self.db.create_user("test@example.com", "User Two", "pass2")
        assert user_id2 is None

    def test_database_connection_issues(self):
        """测试数据库连接"""
        db = AuthDatabase()
        assert db is not None
        assert db.get_connection() is not None


class TestUserIdGeneration:
    """测试用户ID生成算法"""

    def setup_method(self):
        """测试前初始化"""
        self.db = AuthDatabase()

    def test_basic_email_id_generation(self):
        """测试基本邮箱ID生成"""
        email = "test@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "test"

        email = "user123@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "user123"

    def test_email_with_special_characters(self):
        """测试包含特殊字符的邮箱"""
        email = "test.user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"

        email = "test_user@example.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "testuser"

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
        email = "a@example.com"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3
        assert user_id.startswith("a")

        email = "ab@example.com"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3
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
        email = "用户@example.com"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3

        email = "user123@例子.com"
        user_id = self.db._generate_user_id(email)
        assert user_id == "user123"

    def test_empty_and_invalid_emails(self):
        """测试空和无效邮箱地址"""
        email = ""
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3

        email = "invalid-email"
        user_id = self.db._generate_user_id(email)
        assert len(user_id) >= 3

    def test_user_id_uniqueness(self):
        """测试用户ID唯一性处理"""
        email1 = "test@example.com"
        email2 = "test@example.org"

        user_id_base1 = self.db._generate_user_id(email1)
        user_id_base2 = self.db._generate_user_id(email2)

        assert user_id_base1 == user_id_base2 == "test"


class TestUserDatabaseOperations:
    """测试用户数据库操作"""

    def setup_method(self):
        """测试前初始化"""
        self.db = AuthDatabase()

    def teardown_method(self):
        """测试后清理"""
        if self.db:
            self.db.close()

    def test_user_creation_with_generated_id(self):
        """测试使用生成ID创建用户"""
        email = "test@example.com"
        password = "password123"
        name = "Test User"

        user_id = self.db.create_user(email, name, password)
        assert user_id is not None
        assert user_id == "test"

        user_info = self.db.get_user_by_id(user_id)
        assert user_info["email"] == email
        assert user_info["name"] == name

    def test_user_id_collision_handling(self):
        """测试用户ID冲突处理"""
        email1 = "test@example.com"
        user_id1 = self.db.create_user(email1, "User One", "pass1")
        assert user_id1 == "test"

        email2 = "test@example.org"
        user_id2 = self.db.create_user(email2, "User Two", "pass2")

        assert user_id2 != user_id1
        assert user_id2.startswith("test")
        assert user_id2.endswith("1")

    def test_get_user_id_by_email(self):
        """测试根据邮箱获取用户ID"""
        email = "test@example.com"
        expected_user_id = "test"

        self.db.create_user(email, "Test User", "password")

        user_id = self.db.get_user_id_by_email(email)
        assert user_id == expected_user_id

    def test_get_user_by_email(self):
        """测试根据邮箱获取用户信息"""
        email = "test@example.com"
        name = "Test User"

        self.db.create_user(email, name, "password")

        user_info = self.db.get_user_by_email(email)
        assert user_info["email"] == email
        assert user_info["name"] == name
        assert user_info["id"] == "test"

    def test_duplicate_email_prevention(self):
        """测试重复邮箱注册预防"""
        email = "test@example.com"

        user_id1 = self.db.create_user(email, "User One", "pass1")
        assert user_id1 is not None

        user_id2 = self.db.create_user(email, "User Two", "pass2")
        assert user_id2 is None


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
