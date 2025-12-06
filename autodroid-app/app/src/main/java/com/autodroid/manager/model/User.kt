package com.autodroid.manager.model

/**
 * 用户信息类，封装登录成功的用户数据
 * 这是一个数据类，用于在全局共享状态中存储用户认证信息
 */
data class User(
    val userId: String? = null,
    val email: String? = null,
    val token: String? = null,
    val isAuthenticated: Boolean = false
) {
    /**
     * 检查用户信息是否有效
     */
    fun isValid(): Boolean {
        return isAuthenticated && !userId.isNullOrBlank() && !email.isNullOrBlank() && !token.isNullOrBlank()
    }
    
    /**
     * 创建空的用户信息（用于登出或初始化状态）
     */
    companion object {
        fun empty(): User {
            return User(isAuthenticated = false)
        }
    }
}