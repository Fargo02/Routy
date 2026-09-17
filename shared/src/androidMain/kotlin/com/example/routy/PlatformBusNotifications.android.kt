package com.example.routy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

private const val BusChannelId = "bus-arrival-alerts"
private var notificationContext: Context? = null

fun initializeBusNotifications(context: Context) {
    notificationContext = context
}

actual fun requestBusNotificationPermission() {
    val activity = notificationContext as? Activity ?: return
    if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 404)
    }
}

actual fun notifyBusApproaching(title: String, body: String) {
    val context = notificationContext ?: return
    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(NotificationChannel(BusChannelId, "Bus arrival alerts", NotificationManager.IMPORTANCE_HIGH))
    }
    manager.notify(
        body.hashCode(),
        android.app.Notification.Builder(context, BusChannelId)
            .setSmallIcon(com.example.routy.shared.R.drawable.ic_notification_routy)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build(),
    )
}
