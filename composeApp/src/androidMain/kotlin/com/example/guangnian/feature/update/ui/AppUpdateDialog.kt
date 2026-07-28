package com.example.guangnian.feature.update.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.guangnian.R

/**
 * @author 光年
 * @summary 应用更新弹窗
 */
class AppUpdateDialog(private val context: Context) {

    private var dialog: AlertDialog? = null
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView
    private lateinit var layoutProgress: LinearLayout
    private lateinit var pbDownload: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnUpdate: Button

    fun show(
        versionName: String,
        updateDesc: String,
        isForceUpdate: Boolean,
        onUpdateClick: () -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_app_update, null)
        tvTitle = view.findViewById(R.id.tv_update_title)
        tvContent = view.findViewById(R.id.tv_update_content)
        layoutProgress = view.findViewById(R.id.layout_progress)
        pbDownload = view.findViewById(R.id.pb_download)
        tvProgress = view.findViewById(R.id.tv_progress)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnUpdate = view.findViewById(R.id.btn_update)

        tvTitle.text = "发现新版本 V$versionName"
        tvContent.text = updateDesc.ifEmpty { "优化部分已知问题，提升用户体验。" }

        if (isForceUpdate) {
            btnCancel.visibility = View.GONE
        }

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(!isForceUpdate)
            .create()

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog?.dismiss() }
        btnUpdate.setOnClickListener {
            btnUpdate.isEnabled = false
            btnUpdate.text = "下载中..."
            btnCancel.isEnabled = false
            layoutProgress.visibility = View.VISIBLE
            onUpdateClick.invoke()
        }
        dialog?.show()
    }

    fun updateProgress(progress: Int) {
        pbDownload.progress = progress
        tvProgress.text = "$progress%"
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}