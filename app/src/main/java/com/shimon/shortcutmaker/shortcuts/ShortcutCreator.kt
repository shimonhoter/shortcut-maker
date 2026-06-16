package com.shimon.shortcutmaker.shortcuts

// [START: ShortcutLogic]

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.shimon.shortcutmaker.data.ShortcutConfig
import com.shimon.shortcutmaker.data.ShortcutType

object ShortcutCreator {

    fun pinShortcut(context: Context, config: ShortcutConfig): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false

        val intent = buildIntent(context, config)
        val shortcutInfo = ShortcutInfoCompat.Builder(context, config.id)
            .setShortLabel(config.label.take(10))
            .setLongLabel(config.label.take(25))
            .setIcon(iconForType(context, config.type))
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        return true
    }

    fun removeShortcut(context: Context, id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }

    fun buildIntent(context: Context, config: ShortcutConfig): Intent {
        return when (config.type) {

            ShortcutType.DIAL -> {
                // Must go through ShortcutActionActivity for runtime permission
                Intent(context, ShortcutActionActivity::class.java).apply {
                    action = Intent.ACTION_VIEW  // shortcuts need an action
                    putExtra(ShortcutActionActivity.EXTRA_TYPE, "DIAL")
                    putExtra(ShortcutActionActivity.EXTRA_PHONE, config.phoneNumber)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }

            ShortcutType.NAVIGATE_MAPS -> {
                val uri = Uri.parse(
                    "google.navigation:q=${config.latitude},${config.longitude}&mode=d"
                )
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            ShortcutType.NAVIGATE_WAZE -> {
                val uri = Uri.parse(
                    "waze://?ll=${config.latitude},${config.longitude}&navigate=yes"
                )
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            ShortcutType.OPEN_URL -> {
                val url = if (config.url.startsWith("http")) config.url else "https://${config.url}"
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            ShortcutType.OPEN_APP -> {
                context.packageManager
                    .getLaunchIntentForPackage(config.packageName)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            }

            ShortcutType.SEND_SMS -> {
                Intent(context, ShortcutActionActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(ShortcutActionActivity.EXTRA_TYPE, "SMS")
                    putExtra(ShortcutActionActivity.EXTRA_PHONE, config.phoneNumber)
                    putExtra(ShortcutActionActivity.EXTRA_SMS_BODY, config.smsBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            }
        }
    }

    private fun iconForType(context: Context, type: ShortcutType): IconCompat {
        val resId = when (type) {
            ShortcutType.DIAL           -> android.R.drawable.ic_menu_call
            ShortcutType.NAVIGATE_MAPS  -> android.R.drawable.ic_menu_compass
            ShortcutType.NAVIGATE_WAZE  -> android.R.drawable.ic_menu_directions
            ShortcutType.OPEN_URL       -> android.R.drawable.ic_menu_search
            ShortcutType.OPEN_APP       -> android.R.drawable.ic_menu_more
            ShortcutType.SEND_SMS       -> android.R.drawable.ic_dialog_email
        }
        return IconCompat.createWithResource(context, resId)
    }
}

// [END: ShortcutLogic]
