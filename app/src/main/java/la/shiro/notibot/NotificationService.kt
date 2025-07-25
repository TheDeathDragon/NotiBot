package la.shiro.notibot

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.*
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
        
        // 检查是否需要恢复随机通知
        val sharedPrefs = getSharedPreferences("NotiBot", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("random_notification_running", false) && !isRandomNotificationActive) {
            val interval = sharedPrefs.getLong("random_notification_interval", 10000)
            val intent = Intent().apply {
                putExtra(EXTRA_INTERVAL, interval)
            }
            startRandomNotifications(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_NOW -> sendNotificationNow(intent)
            ACTION_SEND_DELAYED -> sendDelayedNotification(intent)
            ACTION_START_RANDOM -> startRandomNotifications(intent)
            ACTION_STOP_RANDOM -> stopRandomNotifications()
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
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        val importance = intent.getIntExtra(EXTRA_IMPORTANCE, 1)
        
        showNotification(title, content, importance)
    }

    private fun sendDelayedNotification(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_delayed_title)
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_delayed_content)
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

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        showNotification(title, content, importance)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRandomNotifications()
    }
}