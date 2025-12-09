// APKScannerManager.kt
package com.autodroid.manager.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.autodroid.manager.service.NetworkService
import com.google.gson.Gson
import java.util.*

class APKScannerManager(private val context: Context) {
    private val gson: Gson

    init {
        this.gson = Gson()
    }

    fun scanInstalledApks() {
        Thread(Runnable {
            try {
                val installedApks: MutableList<MutableMap<String?, Any?>?> =
                    ArrayList<MutableMap<String?, Any?>?>()
                simulateInstalledApks(installedApks)
                sendApkInfoToServer(installedApks)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan installed APKs: " + e.message)
            }
        }).start()
    }

    private fun simulateInstalledApks(installedApks: MutableList<MutableMap<String?, Any?>?>) {
        val apk1: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        apk1.put("app_name", "Sample App")
        apk1.put("package_name", "com.sample.app")
        apk1.put("version_name", "1.0.0")
        installedApks.add(apk1)

        val apk2: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        apk2.put("app_name", "Another App")
        apk2.put("package_name", "com.another.app")
        apk2.put("version_name", "2.1.0")
        installedApks.add(apk2)
    }

    private fun sendApkInfoToServer(installedApks: MutableList<MutableMap<String?, Any?>?>?) {
        val apkInfoJson = gson.toJson(installedApks)
        val intent = Intent(context, NetworkService::class.java)
        intent.setAction("MATCH_WORKSCRIPTS")
        intent.putExtra("apk_info_list", apkInfoJson)
        context.startService(intent)
    }

    companion object {
        private const val TAG = "APKScannerManager"
    }
}