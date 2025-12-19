package com.autodroid.trader.ui.my

import com.autodroid.trader.ui.my.MyAdapter

sealed class MyItem(val type: Int) {
    data class ItemLogin(
        val title: String = "登录",
        val description: String = "点击登录您的账户"
    ) : MyItem(MyAdapter.Companion.TYPE_LOGIN)

    data class ItemLogout(
        val title: String = "退出登录",
        val description: String = "安全退出当前账户"
    ) : MyItem(MyAdapter.Companion.TYPE_LOGOUT)

    data class ItemUserQRCode(
        val title: String = "我的二维码",
        val description: String = "查看和分享您的个人二维码"
    ) : MyItem(MyAdapter.Companion.TYPE_USER_QR_CODE)
}