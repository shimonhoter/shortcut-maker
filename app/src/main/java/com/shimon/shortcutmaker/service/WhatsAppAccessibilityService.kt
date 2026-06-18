package com.shimon.shortcutmaker.service

// [START: WhatsAppAccessibilityService]
// שירות נגישות שמשלים את שליחת הודעת WhatsApp באופן אוטומטי.
// כיוון העבודה: SchedulerReceiver פותח את WhatsApp עם טקסט מוכן בתיבת ההקלדה
// (דרך wa.me deep-link), והשירות הזה מאתר את כפתור השליחה ולוחץ עליו אוטומטית
// כדי שההודעה תישלח בלי מעורבות אנושית - בדיוק כמו שמשתמש היה עושה בעצמו.

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "WhatsAppA11yService"
        const val WHATSAPP_PACKAGE = "com.whatsapp"

        // דגל גלובלי שמסמן "יש משימה ממתינה לשליחה אוטומטית".
        // נקבע ל-true ע"י SchedulerReceiver ממש לפני פתיחת WhatsApp,
        // ומתאפס ל-false אחרי שליחה מוצלחת או timeout - כדי שהשירות
        // לא ילחץ אוטומטית על "שלח" כשהמשתמש פותח את WhatsApp בעצמו ידנית.
        @Volatile
        var pendingAutoSend: Boolean = false

        @Volatile
        var onSendResult: ((Boolean) -> Unit)? = null

        private val handler = Handler(Looper.getMainLooper())

        fun armAutoSend(timeoutCallback: (Boolean) -> Unit) {
            pendingAutoSend = true
            onSendResult = timeoutCallback
            // אם תוך 12 שניות לא הצלחנו ללחוץ על שלח (למשל מבנה UI השתנה
            // או WhatsApp לא נפתח), נדווח כשלון כדי שהסטטוס יתעדכן ל-FAILED
            handler.postDelayed({
                if (pendingAutoSend) {
                    Log.w(TAG, "Auto-send timed out - WhatsApp send button not found in time")
                    pendingAutoSend = false
                    onSendResult?.invoke(false)
                    onSendResult = null
                }
            }, 12_000)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!pendingAutoSend) return
        if (event == null) return
        if (event.packageName?.toString() != WHATSAPP_PACKAGE) return

        // רק על אירועי שינוי תוכן/חלון רלוונטיים - לא על כל אירוע (חיסכון בסוללה/ביצועים)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val root = rootInActiveWindow ?: return
        try {
            val sendButton = findSendButton(root)
            if (sendButton != null) {
                val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Send button found, click performed=$clicked")
                if (clicked) {
                    pendingAutoSend = false
                    onSendResult?.invoke(true)
                    onSendResult = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while searching for send button: ${e.message}")
        } finally {
            root.recycle()
        }
    }

    /**
     * מאתר את כפתור השליחה במסך הצ'אט של WhatsApp.
     * WhatsApp מזהה את הכפתור עם content-description "Send" (אנגלית) או
     * "שלח" (עברית, תלוי בשפת המכשיר), או resource-id המכיל "send".
     */
    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // חיפוש לפי resource-id (הדרך היציבה ביותר, פחות תלויה בשפה)
        val byId = node.findAccessibilityNodeInfosByViewId("$WHATSAPP_PACKAGE:id/send")
        if (byId.isNotEmpty()) return byId[0]

        // גיבוי: חיפוש לפי content-description בעברית/אנגלית
        val descriptions = listOf("Send", "שלח")
        for (desc in descriptions) {
            val found = node.findAccessibilityNodeInfosByText(desc)
            for (candidate in found) {
                if (candidate.isClickable) return candidate
                // לפעמים הטקסט נמצא על אלמנט פנימי; ננסה את ה-parent הניתן ללחיצה
                var parent = candidate.parent
                var depth = 0
                while (parent != null && depth < 3) {
                    if (parent.isClickable) return parent
                    parent = parent.parent
                    depth++
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "WhatsApp accessibility service connected")
    }
}

// [END: WhatsAppAccessibilityService]
