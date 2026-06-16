package com.shimon.shortcutmaker.shortcuts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class ShortcutActionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE     = "extra_type"
        const val EXTRA_PHONE    = "extra_phone"
        const val EXTRA_SMS_BODY = "extra_sms_body"
    }

    private var pendingAction: (() -> Unit)? = null

    private val requestCallPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingAction?.invoke()
        else toast("הרשאת חיוג נדחתה")
        finish()
    }

    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingAction?.invoke()
        else toast("הרשאת SMS נדחתה")
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type  = intent.getStringExtra(EXTRA_TYPE) ?: ""
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""

        when (type) {
            "DIAL" -> {
                pendingAction = { makeDial(phone) }
                if (hasPermission(Manifest.permission.CALL_PHONE)) {
                    makeDial(phone)
                } else {
                    requestCallPermission.launch(Manifest.permission.CALL_PHONE)
                }
            }
            "SMS" -> {
                val body = intent.getStringExtra(EXTRA_SMS_BODY) ?: ""
                pendingAction = { sendSms(phone, body); finish() }
                if (hasPermission(Manifest.permission.SEND_SMS)) {
                    sendSms(phone, body)
                    finish()
                } else {
                    requestSmsPermission.launch(Manifest.permission.SEND_SMS)
                }
            }
            else -> finish()
        }
    }

    private fun makeDial(phone: String) {
        // ACTION_CALL requires permission; use ACTION_DIAL as fallback-safe
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${phone.trim()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(callIntent)
        } catch (e: Exception) {
            // fallback to dialer UI
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.trim()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        }
        finish()
    }

    private fun sendSms(phone: String, body: String) {
        try {
            @Suppress("DEPRECATION")
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            // Split long messages automatically
            val parts = smsManager.divideMessage(body)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phone.trim(), null, body, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phone.trim(), null, parts, null, null)
            }
            toast("הודעה נשלחה ✓")
        } catch (e: Exception) {
            toast("שגיאה: ${e.message}")
        }
    }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
