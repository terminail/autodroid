package com.autodroid.guardiansdk.ui.contacts.model

sealed class ContactItem {
    data class ContactCard(
        val phoneNumber: String,
        val name: String,
        val contactType: String = "",
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : ContactItem()

    data class ContactListItem(
        val phoneNumber: String,
        val name: String,
        val contactType: String = "",
        val avatar: String = "",
        val lastContactTime: String,
        val lastLocation: String,
        val lastAlarmMessage: String
    ) : ContactItem()

    data class AddContactButton(
        val index: Int
    ) : ContactItem()

    data class EmptyState(
        val message: String = "还没有添加联系人"
    ) : ContactItem()
}
