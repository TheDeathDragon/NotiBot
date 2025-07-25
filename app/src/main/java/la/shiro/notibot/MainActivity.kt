package la.shiro.notibot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import androidx.core.net.toUri
import la.shiro.notibot.databinding.ActivityMainBinding
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPreferences
    private var isRandomNotificationRunning = false

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 100
        const val CHANNEL_ID_HIGH = "high_importance_channel"
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_LOW = "low_importance_channel"
        const val CHANNEL_ID_MIN = "min_importance_channel"
        const val PREF_NAME = "NotiBot"
        const val PREF_RANDOM_RUNNING = "random_notification_running"
        const val PREF_RANDOM_INTERVAL = "random_notification_interval"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        
        setupSpinner()
        setupClickListeners()
        createNotificationChannels()
        requestPermissions()
        restoreState()
    }

    private fun restoreState() {
        isRandomNotificationRunning = sharedPrefs.getBoolean(PREF_RANDOM_RUNNING, false)
        if (isRandomNotificationRunning) {
            binding.startRandomButton.isEnabled = false
            binding.stopRandomButton.isEnabled = true
            updateStatus(getString(R.string.status_random_running))
        } else {
            binding.startRandomButton.isEnabled = true
            binding.stopRandomButton.isEnabled = false
        }
    }

    private fun setupSpinner() {
        val importanceLevels = arrayOf(
            getString(R.string.importance_high),
            getString(R.string.importance_default),
            getString(R.string.importance_low),
            getString(R.string.importance_min)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, importanceLevels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.importanceSpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.sendNowButton.setOnClickListener { sendNotificationNow() }
        binding.sendDelayedButton.setOnClickListener { sendDelayedNotification() }
        binding.startRandomButton.setOnClickListener { startRandomNotifications() }
        binding.stopRandomButton.setOnClickListener { stopRandomNotifications() }
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // 高重要性通道
        val highChannel = NotificationChannel(
            CHANNEL_ID_HIGH,
            getString(R.string.channel_high_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_high_description)
            enableVibration(true)
        }

        // 默认通道
        val defaultChannel = NotificationChannel(
            CHANNEL_ID_DEFAULT,
            getString(R.string.channel_default_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_default_description)
        }

        // 低重要性通道
        val lowChannel = NotificationChannel(
            CHANNEL_ID_LOW,
            getString(R.string.channel_low_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_low_description)
        }

        // 最小重要性通道（静默）
        val minChannel = NotificationChannel(
            CHANNEL_ID_MIN,
            getString(R.string.channel_min_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_min_description)
        }

        notificationManager.createNotificationChannels(
            listOf(highChannel, defaultChannel, lowChannel, minChannel)
        )
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, 
                permissionsToRequest.toTypedArray(), 
                NOTIFICATION_PERMISSION_REQUEST
            )
        }

        // 请求忽略电池优化
        requestBatteryOptimizationExemption()
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        }
    }

    private fun sendNotificationNow() {
        val title = binding.titleEditText.text.toString().ifEmpty { getString(R.string.default_notification_title) }
        val content = binding.contentEditText.text.toString().ifEmpty { getString(R.string.default_notification_content) }
        val importance = binding.importanceSpinner.selectedItemPosition

        val intent = Intent(this, NotificationService::class.java).apply {
            action = NotificationService.ACTION_SEND_NOW
            putExtra(NotificationService.EXTRA_TITLE, title)
            putExtra(NotificationService.EXTRA_CONTENT, content)
            putExtra(NotificationService.EXTRA_IMPORTANCE, importance)
        }
        
        ContextCompat.startForegroundService(this, intent)
        updateStatus(getString(R.string.status_notification_sent))
    }

    private fun sendDelayedNotification() {
        val title = binding.titleEditText.text.toString().ifEmpty { getString(R.string.default_delayed_title) }
        val content = binding.contentEditText.text.toString().ifEmpty { getString(R.string.default_delayed_content) }
        val importance = binding.importanceSpinner.selectedItemPosition
        val delay = binding.delayEditText.text.toString().toLongOrNull() ?: 5

        val intent = Intent(this, NotificationService::class.java).apply {
            action = NotificationService.ACTION_SEND_DELAYED
            putExtra(NotificationService.EXTRA_TITLE, title)
            putExtra(NotificationService.EXTRA_CONTENT, content)
            putExtra(NotificationService.EXTRA_IMPORTANCE, importance)
            putExtra(NotificationService.EXTRA_DELAY, delay * 1000) // 转换为毫秒
        }
        
        ContextCompat.startForegroundService(this, intent)
        updateStatus(getString(R.string.status_delayed_notification_set, delay.toInt()))
    }

    private fun startRandomNotifications() {
        val interval = binding.randomIntervalEditText.text.toString().toLongOrNull() ?: 10

        val intent = Intent(this, NotificationService::class.java).apply {
            action = NotificationService.ACTION_START_RANDOM
            putExtra(NotificationService.EXTRA_INTERVAL, interval * 1000) // 转换为毫秒
        }
        
        ContextCompat.startForegroundService(this, intent)
        updateStatus(getString(R.string.status_random_started, interval.toInt()))
        
        isRandomNotificationRunning = true
        sharedPrefs.edit {
            putBoolean(PREF_RANDOM_RUNNING, true)
                .putLong(PREF_RANDOM_INTERVAL, interval * 1000)
        }
        
        binding.startRandomButton.isEnabled = false
        binding.stopRandomButton.isEnabled = true
    }

    private fun stopRandomNotifications() {
        val intent = Intent(this, NotificationService::class.java).apply {
            action = NotificationService.ACTION_STOP_RANDOM
        }
        
        startService(intent)
        updateStatus(getString(R.string.status_random_stopped))
        
        isRandomNotificationRunning = false
        sharedPrefs.edit { putBoolean(PREF_RANDOM_RUNNING, false) }
        
        binding.startRandomButton.isEnabled = true
        binding.stopRandomButton.isEnabled = false
    }

    private fun updateStatus(message: String) {
        binding.statusTextView.text = getString(R.string.status_label, message)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                updateStatus(getString(R.string.status_permissions_granted))
            } else {
                updateStatus(getString(R.string.status_permissions_denied))
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                showInfoDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showInfoDialog() {
        val versionName = BuildConfig.VERSION_NAME
        val buildTime = BuildConfig.BUILD_TIME
        val appName = BuildConfig.APP_NAME
        
        val message = getString(R.string.about_message, appName, versionName, buildTime)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
}