package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FirebaseSafeAccess
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NightCoffeeMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        val uid = FirebaseSafeAccess.safeAuth(applicationContext)?.currentUser?.uid ?: return
        val db = FirebaseSafeAccess.safeFirestore(applicationContext) ?: return
        serviceScope.launch {
            runCatching {
                db.collection("user_preferences").document(uid)
                    .set(
                        mapOf(
                            "userId" to uid,
                            "fcmToken" to token,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }.onFailure { err ->
                Log.w(TAG, "Failed to update FCM token in user_preferences: ${err.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        val targetRoute = message.data["targetRoute"].orEmpty()
        val targetId = message.data["targetId"].orEmpty()

        showNotification(
            context = applicationContext,
            title = title,
            body = body,
            targetRoute = targetRoute,
            targetId = targetId
        )
    }

    companion object {
        private const val TAG = "NightCoffeeFCM"
        const val EXTRA_TARGET_ROUTE = "extra_target_route"
        const val EXTRA_TARGET_ID = "extra_target_id"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = context.getString(R.string.default_notification_channel_id)
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notifications_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notifications_subtitle)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun showNotification(
            context: Context,
            title: String,
            body: String,
            targetRoute: String,
            targetId: String
        ) {
            if (!hasNotificationPermission(context)) return
            ensureNotificationChannel(context)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_ROUTE, targetRoute)
                putExtra(EXTRA_TARGET_ID, targetId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (targetRoute + targetId).hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = context.getString(R.string.default_notification_channel_id)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_coffee)
                .setColor(ContextCompat.getColor(context, R.color.caramel_gold))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            runCatching {
                NotificationManagerCompat.from(context)
                    .notify((System.currentTimeMillis() and 0x7FFFFFFF).toInt(), notification)
            }
        }
    }
}
