package la.shiro.notibot

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import la.shiro.notibot.databinding.ActivityFullScreenNotificationBinding

class FullScreenNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenNotificationBinding
    private var notificationId: Int = -1

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityFullScreenNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title =
            intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.default_notification_title)
        val content =
            intent.getStringExtra(EXTRA_CONTENT) ?: getString(R.string.default_notification_content)
        notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        binding.notificationTitle.text = title
        binding.notificationContent.text = content

        binding.dismissButton.setOnClickListener {
            dismissNotification()
        }

        binding.snoozeButton.setOnClickListener {
            dismissNotification()
        }
    }

    private fun dismissNotification() {
        if (notificationId != -1) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
        finish()
    }
}
