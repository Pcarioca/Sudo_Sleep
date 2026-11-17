package com.example.sudosleep.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.sudosleep.MainActivity
import com.example.sudosleep.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.ensureChannelsCreated(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Upcoming Class Reminder 📚"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: "Starting soon!"
        val isExam = intent.getBooleanExtra(EXTRA_IS_EXAM, false)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isExam) {
            NotificationHelper.CHANNEL_EXAMS
        } else {
            NotificationHelper.CHANNEL_CLASSES
        }

        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(Notification.BigTextStyle().bigText(subtitle))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_IS_EXAM = "extra_is_exam"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
