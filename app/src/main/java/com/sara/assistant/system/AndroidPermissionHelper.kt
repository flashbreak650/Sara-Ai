package com.sara.assistant.system

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class AndroidPermissionSnapshot(
    val microphoneGranted: Boolean,
    val callPhoneGranted: Boolean,
    val sendSmsGranted: Boolean,
    val notificationsGranted: Boolean,
    val appLaunchingSupported: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationAccessEnabled: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val speechRecognitionAvailable: Boolean,
    val deviceLockSupported: Boolean,
    val screenCaptureSupported: Boolean
)

/**
 * Queries real Android system permission & capability states.
 * Never simulates or fakes granted permissions.
 */
object AndroidPermissionHelper {

    fun captureSnapshot(context: Context): AndroidPermissionSnapshot {
        val mic = isGranted(context, Manifest.permission.RECORD_AUDIO)
        val call = isGranted(context, Manifest.permission.CALL_PHONE)
        val sms = isGranted(context, Manifest.permission.SEND_SMS)
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val canLaunchApps = context.packageManager.queryIntentActivities(launcherIntent, 0).isNotEmpty()

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val accessibilityEnabled = enabledServices.contains(context.packageName, ignoreCase = true)

        val listenerPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val notifAccess = listenerPackages.contains(context.packageName)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        val speechAvailable = SpeechRecognizer.isRecognitionAvailable(context)

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val deviceSecure = keyguardManager?.isDeviceSecure == true

        return AndroidPermissionSnapshot(
            microphoneGranted = mic,
            callPhoneGranted = call,
            sendSmsGranted = sms,
            notificationsGranted = notif,
            appLaunchingSupported = canLaunchApps,
            accessibilityEnabled = accessibilityEnabled,
            notificationAccessEnabled = notifAccess,
            batteryOptimizationIgnored = batteryIgnored,
            speechRecognitionAvailable = speechAvailable,
            deviceLockSupported = deviceSecure,
            screenCaptureSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        )
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val requestIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val started = runCatching { context.startActivity(requestIntent) }.isSuccess
        if (!started) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(fallback) }
        }
    }
}
