package com.agileprobd.bloud
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class MusicPlaybackService : Service(), MediaPlayer.OnCompletionListener {
    private val TAG = "MusicPlaybackService"
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private val NOTIFICATION_CHANNEL_ID = "MusicPlaybackChannel"
    private val NOTIFICATION_ID = 100 // Unique ID for the foreground notification

    // Define actions for Intents
    companion object {
        const val ACTION_PLAY_MUSIC = "com.agileprobd.bloud.ACTION_PLAY_MUSIC"
        const val ACTION_STOP_MUSIC = "com.agileprobd.bloud.ACTION_STOP_MUSIC"
    }

    override fun onCreate() {
        super.onCreate(

        )
        Log.d(TAG, "Service created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel() // Create notification channel for Android O+
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_PLAY_MUSIC -> {
                startMusicPlayback()
            }
            ACTION_STOP_MUSIC -> {
                stopMusicPlayback()
            }
            else -> {
                // If service is started without a specific action, stop it gracefully
                stopSelf()
            }
        }
        // START_STICKY means if the service is killed by the system, it will be
        // recreated, but the last intent is NOT redelivered. Good for media playback.
        return START_STICKY
    }

    private fun startMusicPlayback() {
        if (mediaPlayer == null) {
            // Create MediaPlayer instance only if it's null
            mediaPlayer = MediaPlayer.create(this, R.raw.music) // Ensure R.raw.music exists!
            mediaPlayer?.setOnCompletionListener(this) // Set this service as the completion listener

            // Set music volume to max (optional, but requested in your original snippet)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )

            // Start the service as a foreground service
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start() // Start or resume playback
            Log.i(TAG, "Music started/resumed")
            Toast.makeText(this, "Music started.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMusicPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop() // Stop playback
            }
            release() // Release resources
            Log.i(TAG, "Music stopped and released")
           Toast.makeText(this@MusicPlaybackService, "Music stopped.", Toast.LENGTH_SHORT).show()
        }
        mediaPlayer = null // Clear reference
        stopForeground(true) // Remove the notification and stop foreground state
        stopSelf() // Stop the service
    }

    override fun onCompletion(mp: MediaPlayer?) {
        // This is called when the media finishes playing
        Log.d(TAG, "Music playback completed - releasing resources")
        Toast.makeText(this, "Music playback completed.", Toast.LENGTH_SHORT).show()
        stopMusicPlayback() // Stop the service when music completes
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure MediaPlayer is released if service is destroyed for any reason
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service won't be bound, so return null
        return null
    }

    // --- Notification Channel for Android O (API 26) and above ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Playback Channel",
                NotificationManager.IMPORTANCE_LOW // Use IMPORTANCE_LOW for ongoing background tasks
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // --- Build the Foreground Notification ---
    private fun buildNotification(): Notification {
        // Intent to open MainActivity when the notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SmsRingerControl Music")
            .setContentText("Music is playing in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's small icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Match channel importance
            .setOngoing(true) // Makes the notification non-dismissible
            .build()
    }
}