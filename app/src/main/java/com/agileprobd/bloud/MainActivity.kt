package com.agileprobd.bloud
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
import android.provider.Settings // Import this

class MainActivity : AppCompatActivity() {

    private val TAG = "SMSControlApp"
    private val PERMISSION_REQUEST_CODE = 100
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_RINGER_MODE = "ringerMode" // "loud" or "silent"

    private lateinit var phoneNumberEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var ringerModeSpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components using lateinit to avoid nullability issues after inflate
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        passphraseEditText = findViewById(R.id.passphraseEditText)
        ringerModeSpinner = findViewById(R.id.ringerModeSpinner)
        saveButton = findViewById(R.id.saveButton)

        // Setup Spinner with options
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.ringer_actions, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ringerModeSpinner.adapter = adapter

        loadSavedSettings()

        // --- NEW: Check for Do Not Disturb Access ---
        checkDndAccess()

        saveButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            val selectedMode = ringerModeSpinner.selectedItem.toString()

            if (phoneNumber.isNotBlank()) {
                val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(KEY_PHONE_NUMBER, phoneNumber)
                    putString(KEY_RINGER_MODE, selectedMode)
                    apply()
                }
                Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSavedSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val savedPhoneNumber = sharedPrefs.getString(KEY_PHONE_NUMBER, "")
        phoneNumberEditText.setText(savedPhoneNumber)

        val savedRingerMode = sharedPrefs.getString(KEY_RINGER_MODE, "Loud")

        val ringerModes = arrayOf("Loud", "Silent")
        val defaultModeIndex = ringerModes.indexOf(savedRingerMode)
        if (defaultModeIndex != -1) {
            ringerModeSpinner.setSelection(defaultModeIndex)
        }
    }

    // --- NEW: Function to check and request DnD access ---
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