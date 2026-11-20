package com.sara.assistant.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sara.assistant.MainActivity
import com.sara.assistant.data.settings.SaraSettingsRepository

/**
 * Restores SARA's background assistant service automatically after device reboot or app update
 * when wakeWordEnabled && autoRestoreOnBoot are saved in DataStore.
 */
class SaraBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val repository = SaraSettingsRepository.getInstance(context.applicationContext)
        val settings = repository.settings.value

        if (!settings.wakeWordEnabled || !settings.autoRestoreOnBoot) {
            return
        }

        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) return

        val started = SaraWakeService.start(
            context = context.applicationContext,
            wakeWord = settings.customWakeWord,
            startPaused = settings.wakeListeningPaused
        )

        // Android 15+ may restrict microphone foreground service start from BOOT_COMPLETED;
        // provide a 1-tap restore notification if OS blocked direct auto-start.
        if (!started && settings.backgroundServiceNotificationEnabled) {
            postTapToResumeNotification(context.applicationContext, settings.assistantName.ifBlank { "SARA" })
        }
    }

    private fun postTapToResumeNotification(context: Context, assistantName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SaraWakeService.CHANNEL_ID,
                "SARA Background Assistant",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_RESUME_WAKE_FROM_BOOT, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            2002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SaraWakeService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("$assistantName ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট")
            .setContentText("ফোন রিস্টার্ট হয়েছে — সার্ভিস পুনরায় চালু করতে ট্যাপ করুন")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(2002, notification)
    }

    companion object {
        const val EXTRA_RESUME_WAKE_FROM_BOOT = "extra_resume_wake_from_boot"
    }
}
