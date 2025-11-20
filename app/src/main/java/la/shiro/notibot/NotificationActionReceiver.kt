package la.shiro.notibot

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_LIKE = "la.shiro.notibot.ACTION_LIKE"
        const val ACTION_REPLY = "la.shiro.notibot.ACTION_REPLY"
        const val ACTION_DISMISS = "la.shiro.notibot.ACTION_DISMISS"
        const val ACTION_SHARE = "la.shiro.notibot.ACTION_SHARE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_LIKE -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.action_like_clicked),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ACTION_REPLY -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.action_reply_clicked),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ACTION_DISMISS -> {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.action_dismiss_clicked),
                    Toast.LENGTH_SHORT
                ).show()
            }

            ACTION_SHARE -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.action_share_clicked),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
