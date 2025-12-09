package com.autodroid.manager.model

import com.autodroid.manager.ui.adapters.MyAdapter

sealed class MyItem(val type: Int) {
    data class LoginItem(
        val title: String = "登录",
        val description: String = "点击登录您的账户"
    ) : MyItem(MyAdapter.TYPE_LOGIN)
    
    data class LogoutItem(
        val title: String = "退出登录",
        val description: String = "安全退出当前账户"
    ) : MyItem(MyAdapter.TYPE_LOGOUT)
    
    data class UserQRCodeItem(
        val title: String = "我的二维码",
        val description: String = "查看和分享您的个人二维码"
    ) : MyItem(MyAdapter.TYPE_USER_QR_CODE)
}