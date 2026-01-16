package com.autodroid.guardiansdk.service

import android.content.Context
import android.util.Log
import com.autodroid.guardiansdk.manager.EmailConfigManager
import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * 邮件发送服务
 * 负责发送带有录音附件的报警邮件
 */
class EmailSender(private val context: Context) {

    companion object {
        private const val TAG = "EmailSender"
        private const val DEFAULT_SUBJECT = "Guardian 紧急报警 - 现场录音"
        private const val DEFAULT_BODY = """这是来自 Guardian SDK 的紧急报警邮件。

报警时间：%s
录音时长：%d 分钟
文件名称：%s

请尽快查看附件中的录音文件，了解现场情况。

---
此邮件由 Guardian SDK 自动发送，请勿回复。"""

        private const val CONNECTION_TIMEOUT = 30000 // 30秒连接超时
        private const val SEND_TIMEOUT = 60000 // 60秒发送超时
    }

    private val emailConfigManager = EmailConfigManager(context)

    /**
     * 发送带附件的邮件
     * @param audioFile 录音文件
     * @param recipientEmail 收件人邮箱（如果为null，使用配置的发件人邮箱作为收件人）
     * @return 发送是否成功
     */
    fun sendEmailWithAttachment(
        audioFile: File,
        recipientEmail: String? = null
    ): Boolean {
        val config = emailConfigManager.getEmailConfig()
            ?: run {
                Log.w(TAG, "未配置邮箱信息，无法发送邮件")
                return false
            }

        return try {
            // 创建邮件会话
            val props = Properties().apply {
                put("mail.smtp.host", config.smtpHost)
                put("mail.smtp.port", config.smtpPort)
                put("mail.smtp.auth", "true")
                if (config.useTLS) {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                }
                put("mail.smtp.connectiontimeout", CONNECTION_TIMEOUT.toString())
                put("mail.smtp.timeout", SEND_TIMEOUT.toString())
            }

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.emailAddress, config.password)
                }
            })

            // 创建邮件消息
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.emailAddress))
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail ?: config.emailAddress)
                )
                subject = DEFAULT_SUBJECT

                // 创建多部分消息（正文+附件）
                val multipart = MimeMultipart()

                // 正文部分
                val bodyPart = MimeBodyPart()
                val recordingDurationMinutes = (audioFile.length() / (64000 * 60)).toInt() // 估算录音时长（64kbps）
                val bodyText = String.format(
                    DEFAULT_BODY,
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
                        .format(java.util.Date(audioFile.lastModified())),
                    recordingDurationMinutes,
                    audioFile.name
                )
                bodyPart.setText(bodyText, "UTF-8")
                multipart.addBodyPart(bodyPart)

                // 附件部分
                val attachmentPart = MimeBodyPart()
                val dataSource = FileDataSource(audioFile)
                attachmentPart.dataHandler = DataHandler(dataSource)
                attachmentPart.fileName = audioFile.name
                multipart.addBodyPart(attachmentPart)

                setContent(multipart)
            }

            // 发送邮件（在后台线程执行）
            Thread {
                try {
                    Transport.send(message)
                    Log.i(TAG, "邮件发送成功: ${audioFile.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "邮件发送失败: ${audioFile.name}", e)
                }
            }.start()

            true
        } catch (e: Exception) {
            Log.e(TAG, "创建邮件失败", e)
            false
        }
    }

    /**
     * 检查是否可以发送邮件
     */
    fun canSendEmail(): Boolean {
        return emailConfigManager.isEmailEnabled() &&
                emailConfigManager.getEmailConfig() != null
    }

    /**
     * 获取配置的发件人邮箱
     */
    fun getSenderEmail(): String? {
        return emailConfigManager.getEmailConfig()?.emailAddress
    }
}
