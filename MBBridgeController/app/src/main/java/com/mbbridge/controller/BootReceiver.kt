package com.mbbridge.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机启动接收器
 * 注意：需要在 AndroidManifest.xml 中启用才能使用
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MBBridgeCtrl"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed, starting MBBridge service...")
            context?.let {
                // 可以在这里添加条件判断，比如检查用户设置是否允许开机自启
                MBBridgeService.startService(it)
            }
        }
    }
}
