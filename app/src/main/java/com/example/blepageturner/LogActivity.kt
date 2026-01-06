package com.example.blepageturner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LogActivity : Activity() {

    private lateinit var scroll: ScrollView
    private lateinit var tv: TextView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val line = intent.getStringExtra(ProtocolLogStore.EXTRA_LINE) ?: return
            appendLine(line)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLog.init(this)

        val btnClear = Button(this).apply {
            text = "清空日志"
            setOnClickListener {
                ProtocolLogStore.clear()
                tv.text = ""
            }
        }

        val etAddr = EditText(this).apply {
            hint = "地址过滤(可空)：AA:BB:CC:DD:EE:FF"
            setText(ProtocolLogStore.getScanAddressFilter(this@LogActivity))
        }

        val btnApply = Button(this).apply {
            text = "应用地址过滤"
            setOnClickListener {
                val v = etAddr.text?.toString() ?: ""
                ProtocolLogStore.setScanAddressFilter(this@LogActivity, v)
                AppLog.i("LogActivity", "scan address filter=${ProtocolLogStore.scanAddressFilter}")
                sendBroadcast(Intent(PageTurnerService.ACTION_RESTART_SCAN).setPackage(packageName))
            }
        }

        tv = TextView(this).apply {
            textSize = 12f
            setPadding(24, 24, 24, 24)
        }

        scroll = ScrollView(this).apply {
            addView(tv)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(btnClear)
            addView(etAddr)
            addView(btnApply)
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        setContentView(root)

        // 初次进入先显示内存中的历史日志
        val history = ProtocolLogStore.snapshot()
        if (history.isNotEmpty()) {
            tv.text = history.joinToString(separator = "\n")
            scrollToBottom()
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(receiver, IntentFilter(ProtocolLogStore.ACTION_PROTOCOL_LOG))
    }

    override fun onStop() {
        unregisterReceiver(receiver)
        super.onStop()
    }

    private fun appendLine(line: String) {
        if (tv.text.isNotEmpty()) tv.append("\n")
        tv.append(line)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }
}
