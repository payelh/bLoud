package com.agileprobd.bloud
import android.Manifest
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationManager // Import this
import android.content.BroadcastReceiver
import android.content.Context // Import this
import android.content.Intent // Import this
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings // Import this
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_ACTION = "actionToDo" // "loud" or "silent"

    private val SMS_PERMISSION_REQUEST_CODE = 101
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 102


    private lateinit var phoneNumberEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var actionSpinner: Spinner
    private lateinit var saveButton: Button

    private lateinit var stopMusicButton: AppCompatImageButton

    // NEW: BroadcastReceiver for music playback status updates
    private val musicStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicPlaybackService.ACTION_MUSIC_STARTED -> {
                    stopMusicButton.isEnabled = true
                    stopMusicButton.visibility = View.VISIBLE
                }
                MusicPlaybackService.ACTION_MUSIC_STOPPED -> {
                    stopMusicButton.isEnabled = false
                    stopMusicButton.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components using lateinit to avoid nullability issues after inflate
        initializeUiComponent()

        // Check dnd access
        checkDndAccess()

        //Request permissions
        requestPermissions()

        //Request notification permission
        requestNotificationPermission()

        //Load saved settings
        loadSavedSettings()

    }

    /**
     * Initialize UI components using lateinit to avoid nullability issues after inflate
     */
    private fun initializeUiComponent() {
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        passphraseEditText = findViewById(R.id.passphraseEditText)
        actionSpinner = findViewById(R.id.actionToDoSpinner)
        saveButton = findViewById(R.id.saveButton)
        stopMusicButton = findViewById(R.id.stopMusicButton)
        stopMusicButton.setOnClickListener {
            val stopIntent = Intent(this, MusicPlaybackService::class.java)
            stopIntent.action = MusicPlaybackService.ACTION_STOP_MUSIC
            startService(stopIntent)
        }



        // Setup Spinner with options
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ringer_actions, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        actionSpinner.adapter = adapter

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Save settings to SharedPreferences
     */
    private fun saveSettings() {
        val phoneNumber = phoneNumberEditText.text.toString()
        val passPhrase = passphraseEditText.text.toString()
        val selectedAction = actionSpinner.selectedItem.toString()

        if (phoneNumber.isNotBlank()) {
            val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putString(KEY_PHONE_NUMBER, phoneNumber)
                putString(KEY_PASSPHRASE, passPhrase)
                putString(KEY_ACTION, selectedAction)
                apply()
            }
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private fun loadSavedSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val savedPhoneNumber = sharedPrefs.getString(KEY_PHONE_NUMBER, "")
        phoneNumberEditText.setText(savedPhoneNumber)

        val savedPassPhrase = sharedPrefs.getString(KEY_PASSPHRASE, "")
        passphraseEditText.setText(savedPassPhrase)

        val actionToDo = sharedPrefs.getString(KEY_ACTION, "Loud")

        val actions = arrayOf("Loud", "Play Sound")
        val defaultModeIndex = actions.indexOf(actionToDo)
        if (defaultModeIndex != -1) {
            actionSpinner.setSelection(defaultModeIndex)
        }
    }
    /**
     * Check if Do Not Disturb access is granted and prompt user if not
     */
    private fun checkDndAccess() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Permission not granted, prompt the user
            Toast.makeText(this,
                "Please grant 'Do Not Disturb Access' for SmsRingerControl to change ringer mode.",
                Toast.LENGTH_LONG).show()

            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            // You might want to use startActivityForResult if you need to know when the user returns
            // but for simplicity, startActivity is fine to just open the settings.
        }
    }

    /**
     * Requests necessary runtime permissions (RECEIVE_SMS, READ_SMS, MODIFY_AUDIO_SETTINGS).
     */
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,)

            val allPermissionsGranted = permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }

            if (!allPermissionsGranted) {
                ActivityCompat.requestPermissions(this, permissions, SMS_PERMISSION_REQUEST_CODE)
            } else {
                Log.d(TAG, "All necessary permissions already granted.")
            }
        }
    }

    // NEW: Function to request POST_NOTIFICATIONS permission for Android 13+
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission already granted
                // Toast.makeText(this, "Notification permission already granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "SMS permissions granted.")
                    Toast.makeText(this, "SMS permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "SMS permissions denied.")
                    Toast.makeText(this, "SMS permissions denied. App may not work correctly.", Toast.LENGTH_LONG).show()
                }
            }
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Notification permission granted.")
                } else {
                    Toast.makeText(this, "Notification permission denied. Foreground service notifications may not appear.", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Notification permission denied.Foreground service notifications may not appear.")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // NEW: Register the LocalBroadcastReceiver when the activity starts
        val filter = IntentFilter().apply {
            addAction(MusicPlaybackService.ACTION_MUSIC_STARTED)
            addAction(MusicPlaybackService.ACTION_MUSIC_STOPPED)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(musicStatusReceiver, filter)
    }

    // --- You might also want to re-check the permission when the activity resumes
    // --- in case the user navigates back from settings after granting permission.
    override fun onResume() {
        super.onResume()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            // Permission is now granted
            Toast.makeText(this, "Do Not Disturb Access Granted!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // NEW: Unregister the LocalBroadcastReceiver when the activity stops
        LocalBroadcastManager.getInstance(this).unregisterReceiver(musicStatusReceiver)
    }
}