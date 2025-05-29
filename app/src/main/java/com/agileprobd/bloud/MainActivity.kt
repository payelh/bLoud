package com.agileprobd.bloud

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity:AppCompatActivity()
{
    private val TAG = "SMSControlApp"
    private val PERMISSION_REQUEST_CODE = 100
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_ACTION = "action" // "loud" or "silent"

    private lateinit var phoneNumberEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var actionSpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components using lateinit to avoid nullability issues after inflate
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        passphraseEditText = findViewById(R.id.passphraseEditText)
        actionSpinner = findViewById(R.id.actionSpinner)
        saveButton = findViewById(R.id.saveButton)

        // Setup Spinner with options
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.ringer_actions, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        actionSpinner.adapter = adapter

        // Load previously saved settings
        loadSettings()

        // Set OnClickListener for the Save button using Kotlin's lambda syntax
        saveButton.setOnClickListener {
            saveSettings()
            requestPermissions() // Request permissions when settings are saved
        }

        // Request permissions on app start if not already granted
        requestPermissions()
    }

    /**
     * Loads the saved phone number, passphrase, and action from SharedPreferences
     * and populates the input fields.
     */
    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedPhoneNumber = prefs.getString(KEY_PHONE_NUMBER, "")
        val savedPassphrase = prefs.getString(KEY_PASSPHRASE, "")
        val savedAction = prefs.getString(KEY_ACTION, "loud") // Default to loud

        phoneNumberEditText.setText(savedPhoneNumber)
        passphraseEditText.setText(savedPassphrase)

        // Set spinner selection based on saved action
        val spinnerPosition = (actionSpinner.adapter as ArrayAdapter<String>).getPosition(savedAction?.capitalize())
        if (spinnerPosition >= 0) { // Check if item exists in adapter
            actionSpinner.setSelection(spinnerPosition)
        } else {
            actionSpinner.setSelection(0) // Default to "Loud" if not found
        }
        Log.d(TAG, "Settings loaded: Phone=$savedPhoneNumber, Passphrase=$savedPassphrase, Action=$savedAction")
    }

    /**
     * Saves the current input values for phone number, passphrase, and action
     * to SharedPreferences.
     */
    private fun saveSettings() {
        val phoneNumber = phoneNumberEditText.text.toString().trim()
        val passphrase = passphraseEditText.text.toString().trim()
        val action = actionSpinner.selectedItem.toString().toLowerCase()

        if (phoneNumber.isEmpty() || passphrase.isEmpty()) {
            Toast.makeText(this, "Phone number and passphrase cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_PHONE_NUMBER, phoneNumber)
            putString(KEY_PASSPHRASE, passphrase)
            putString(KEY_ACTION, action)
            apply() // Apply changes asynchronously
        }

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Settings saved: Phone=$phoneNumber, Passphrase=$passphrase, Action=$action")
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
}
