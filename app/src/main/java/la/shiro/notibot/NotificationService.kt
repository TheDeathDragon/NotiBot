package la.shiro.notibot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.Date
import kotlin.random.Random

class NotificationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var randomNotificationRunnable: Runnable? = null
    private var notificationId = 1000

    companion object {
        var isRandomNotificationActive = false
        const val ACTION_SEND_NOW = "action_send_now"
        const val ACTION_SEND_DELAYED = "action_send_delayed"
        const val ACTION_START_RANDOM = "action_start_random"
        const val ACTION_STOP_RANDOM = "action_stop_random"
        const val ACTION_SEND_TWO_ACTIONS = "action_send_two_actions"
        const val ACTION_SEND_ONE_ACTION = "action_send_one_action"
        const val ACTION_SEND_LARGE_ICON = "action_send_large_icon"
        const val ACTION_SEND_FULLSCREEN = "action_send_fullscreen"
        const val ACTION_SEND_FULLSCREEN_ONE_ACTION = "action_send_fullscreen_one_action"
        const val ACTION_SEND_FULLSCREEN_TWO_ACTIONS = "action_send_fullscreen_two_actions"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_IMPORTANCE = "extra_importance"
        const val EXTRA_DELAY = "extra_delay"
        const val EXTRA_INTERVAL = "extra_interval"

        const val SERVICE_CHANNEL_ID = "service_channel"
        const val FOREGROUND_NOTIFICATION_ID = 999
    }

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_NOW -> sendNotificationNow(intent)
            ACTION_SEND_DELAYED -> sendDelayedNotification(intent)
            ACTION_START_RANDOM -> startRandomNotifications(intent)
            ACTION_STOP_RANDOM -> stopRandomNotifications()
            ACTION_SEND_TWO_ACTIONS -> sendNotificationWithTwoActions(intent)
            ACTION_SEND_ONE_ACTION -> sendNotificationWithOneAction(intent)
            ACTION_SEND_LARGE_ICON -> sendNotificationWithLargeIcon(intent)
            ACTION_SEND_FULLSCREEN -> sendFullScreenNotification(intent)
            ACTION_SEND_FULLSCREEN_ONE_ACTION -> sendFullScreenNotificationOneAction(intent)
            ACTION_SEND_FULLSCREEN_TWO_ACTIONS -> sendFullScreenNotificationTwoActions(intent)
        }
        return START_STICKY
    }

    private fun createServiceChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            getString(R.string.channel_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_service_description)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_content))
            .setSmallIcon(R.drawable.ic_info)
            .setContentIntent(createPendingIntent())
            .setOngoing(true)
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendNotificationNow(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        val importance = intent.getIntExtra(EXTRA_IMPORTANCE, 1)

        showNotification(title, content, importance)
    }

    private fun sendDelayedNotification(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_delayed_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_delayed_content)
        val importance = intent.getIntExtra(EXTRA_IMPORTANCE, 1)
        val delay = intent.getLongExtra(EXTRA_DELAY, 5000)

        handler.postDelayed({
            showNotification(title, content, importance)
        }, delay)
    }

    private fun startRandomNotifications(intent: Intent) {
        val interval = intent.getLongExtra(EXTRA_INTERVAL, 10000)

        stopRandomNotifications() // 先停止之前的任务

        isRandomNotificationActive = true

        randomNotificationRunnable = object : Runnable {
            override fun run() {
                showRandomNotification()
                handler.postDelayed(this, interval)
            }
        }

        handler.post(randomNotificationRunnable!!)
    }

    private fun stopRandomNotifications() {
        isRandomNotificationActive = false
        randomNotificationRunnable?.let {
            handler.removeCallbacks(it)
            randomNotificationRunnable = null
        }
    }

    private fun showNotification(title: String, content: String, importance: Int) {
        val channelId = when (importance) {
            0 -> MainActivity.CHANNEL_ID_HIGH
            1 -> MainActivity.CHANNEL_ID_DEFAULT
            2 -> MainActivity.CHANNEL_ID_LOW
            3 -> MainActivity.CHANNEL_ID_MIN
            else -> MainActivity.CHANNEL_ID_DEFAULT
        }

        val priority = when (importance) {
            0 -> NotificationCompat.PRIORITY_HIGH
            1 -> NotificationCompat.PRIORITY_DEFAULT
            2 -> NotificationCompat.PRIORITY_LOW
            3 -> NotificationCompat.PRIORITY_MIN
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_info)
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, notification)
    }

    private fun showRandomNotification() {
        val randomTitles = listOf(
            getString(R.string.random_title_1, Random.nextInt(100)),
            getString(R.string.random_title_2, System.currentTimeMillis() % 1000),
            getString(R.string.random_title_3, Random.nextInt(1000)),
            getString(R.string.random_title_4, Date().toString()),
            getString(R.string.random_title_5, Random.nextInt(10))
        )

        val randomContents = listOf(
            getString(R.string.random_content_1),
            getString(R.string.random_content_2, Date().toString()),
            getString(R.string.random_content_3, Random.nextInt(10000)),
            getString(R.string.random_content_4),
            getString(R.string.random_content_5)
        )

        val title = randomTitles.random()
        val content = randomContents.random()
        val importance = Random.nextInt(4)
        val notificationType = Random.nextInt(7)

        when (notificationType) {
            0 -> showNotification(title, content, importance)
            1 -> showNotificationWithTwoActions(title, content)
            2 -> showNotificationWithOneAction(title, content)
            3 -> showNotificationWithLargeIcon(title, content, importance)
            4 -> showFullScreenNotification(title, content)
            5 -> showFullScreenNotificationOneAction(title, content)
            6 -> showFullScreenNotificationTwoActions(title, content)
        }
    }

    private fun sendNotificationWithTwoActions(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        showNotificationWithTwoActions(title, content)
    }

    private fun sendNotificationWithOneAction(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        showNotificationWithOneAction(title, content)
    }

    private fun sendNotificationWithLargeIcon(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        val importance = intent.getIntExtra(EXTRA_IMPORTANCE, 1)
        showNotificationWithLargeIcon(title, content, importance)
    }

    private fun showNotificationWithTwoActions(title: String, content: String) {
        val channelId = MainActivity.CHANNEL_ID_HIGH
        val priority = NotificationCompat.PRIORITY_HIGH

        val currentNotificationId = notificationId++

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, currentNotificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val likeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_LIKE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val likePendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 1, likeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REPLY
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 2, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(content)
            .setBigContentTitle(title)
            .setSummaryText(getString(R.string.notification_summary_two_actions))

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_info)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setStyle(bigTextStyle)
            .addAction(R.drawable.ic_favorite, getString(R.string.action_like), likePendingIntent)
            .addAction(R.drawable.ic_info, getString(R.string.action_reply), replyPendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(currentNotificationId, notification)
    }

    private fun showNotificationWithOneAction(title: String, content: String) {
        val channelId = MainActivity.CHANNEL_ID_HIGH
        val priority = NotificationCompat.PRIORITY_HIGH

        val currentNotificationId = notificationId++

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, currentNotificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shareIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SHARE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val sharePendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 1, shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(content)
            .setBigContentTitle(title)
            .setSummaryText(getString(R.string.notification_summary_one_action))

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_info)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setStyle(bigTextStyle)
            .addAction(R.drawable.ic_info, getString(R.string.action_share), sharePendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(currentNotificationId, notification)
    }

    private fun showNotificationWithLargeIcon(title: String, content: String, importance: Int) {
        val channelId = getChannelId(importance)
        val priority = getPriority(importance)

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val bigPictureStyle = NotificationCompat.BigPictureStyle()
            .bigPicture(largeIcon)
            .bigLargeIcon(null as android.graphics.Bitmap?)
            .setBigContentTitle(title)
            .setSummaryText(content)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_info)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setStyle(bigPictureStyle)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, notification)
    }

    private fun getChannelId(importance: Int): String {
        return when (importance) {
            0 -> MainActivity.CHANNEL_ID_HIGH
            1 -> MainActivity.CHANNEL_ID_DEFAULT
            2 -> MainActivity.CHANNEL_ID_LOW
            3 -> MainActivity.CHANNEL_ID_MIN
            else -> MainActivity.CHANNEL_ID_DEFAULT
        }
    }

    private fun getPriority(importance: Int): Int {
        return when (importance) {
            0 -> NotificationCompat.PRIORITY_HIGH
            1 -> NotificationCompat.PRIORITY_DEFAULT
            2 -> NotificationCompat.PRIORITY_LOW
            3 -> NotificationCompat.PRIORITY_MIN
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun getRandomLargeIcon(): Int {
        val largeIcons = listOf(
            R.drawable.ic_large_icon,
            R.drawable.ic_large_icon_1,
            R.drawable.ic_large_icon_2,
            R.drawable.ic_large_icon_3,
            R.drawable.ic_large_icon_4,
            R.drawable.ic_large_icon_5
        )
        return largeIcons.random()
    }

    private fun sendFullScreenNotification(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        showFullScreenNotification(title, content)
    }

    private fun sendFullScreenNotificationOneAction(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        showFullScreenNotificationOneAction(title, content)
    }

    private fun sendFullScreenNotificationTwoActions(intent: Intent) {
        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        showFullScreenNotificationTwoActions(title, content)
    }

    private fun showFullScreenNotification(title: String, content: String) {
        val channelId = MainActivity.CHANNEL_ID_HIGH
        val priority = NotificationCompat.PRIORITY_HIGH

        val currentNotificationId = notificationId++

        val fullScreenIntent = Intent(this, FullScreenNotificationActivity::class.java).apply {
            putExtra(FullScreenNotificationActivity.EXTRA_TITLE, title)
            putExtra(FullScreenNotificationActivity.EXTRA_CONTENT, content)
            putExtra(FullScreenNotificationActivity.EXTRA_NOTIFICATION_ID, currentNotificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, currentNotificationId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_timer)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(R.drawable.ic_stop, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(currentNotificationId, notification)
    }

    private fun showFullScreenNotificationOneAction(title: String, content: String) {
        val channelId = MainActivity.CHANNEL_ID_HIGH
        val priority = NotificationCompat.PRIORITY_HIGH

        val currentNotificationId = notificationId++

        val fullScreenIntent = Intent(this, FullScreenNotificationActivity::class.java).apply {
            putExtra(FullScreenNotificationActivity.EXTRA_TITLE, title)
            putExtra(FullScreenNotificationActivity.EXTRA_CONTENT, content)
            putExtra(FullScreenNotificationActivity.EXTRA_NOTIFICATION_ID, currentNotificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, currentNotificationId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_timer)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(R.drawable.ic_stop, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(currentNotificationId, notification)
    }

    private fun showFullScreenNotificationTwoActions(title: String, content: String) {
        val channelId = MainActivity.CHANNEL_ID_HIGH
        val priority = NotificationCompat.PRIORITY_HIGH

        val currentNotificationId = notificationId++

        val fullScreenIntent = Intent(this, FullScreenNotificationActivity::class.java).apply {
            putExtra(FullScreenNotificationActivity.EXTRA_TITLE, title)
            putExtra(FullScreenNotificationActivity.EXTRA_CONTENT, content)
            putExtra(FullScreenNotificationActivity.EXTRA_NOTIFICATION_ID, currentNotificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, currentNotificationId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconResId = getRandomLargeIcon()
        val largeIcon = BitmapFactory.decodeResource(resources, largeIconResId)

        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, currentNotificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, currentNotificationId * 10 + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_timer)
            .setLargeIcon(largeIcon)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(R.drawable.ic_timer, getString(R.string.action_snooze), snoozePendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.action_dismiss), dismissPendingIntent)
            .build()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(currentNotificationId, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRandomNotifications()
    }
}