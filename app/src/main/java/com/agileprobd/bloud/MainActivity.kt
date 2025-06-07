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
import android.content.Context // Import this
import android.content.Intent // Import this
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings // Import this
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_ACTION = "actionToDo" // "loud" or "silent"
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var phoneNumberEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var actionSpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components using lateinit to avoid nullability issues after inflate
        initializeUiComponent()

        // Check dnd access
        checkDndAccess()

        //Request permissions
        requestPermissions()

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
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            )

            val allPermissionsGranted = permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }

            if (!allPermissionsGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            } else {
                Log.d(TAG, "All necessary permissions already granted.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "All permissions granted.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "All permissions granted by user.")
            } else {
                Toast.makeText(this, "Some permissions were denied. App may not function correctly.", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Some permissions denied by user.")
            }
        }
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
}