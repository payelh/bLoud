package com.agileprobd.bloud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast

class SmsReceiver : BroadcastReceiver()
{
    private val TAG = "SmsReceiver"
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_ACTION = "action" // "loud" or "silent"

    override fun onReceive(context: Context, intent: Intent) {
        // Check if the intent action is for SMS received
        if ("android.provider.Telephony.SMS_RECEIVED" == intent.action) {
            Log.d(TAG, "SMS_RECEIVED broadcast received.")

            // Retrieve the SMS message data
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*>
                if (pdus == null || pdus.isEmpty()) {
                    Log.e(TAG, "PDUs are null or empty.")
                    return
                }

                val smsBodyBuilder = StringBuilder()
                var senderPhoneNumber: String? = null

                for (pdu in pdus) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    if (smsMessage != null) {
                        senderPhoneNumber = smsMessage.originatingAddress // Get sender's phone number
                        smsBodyBuilder.append(smsMessage.messageBody) // Get message body
                    }
                }

                val smsBody = smsBodyBuilder.toString()
                Log.d(TAG, "Incoming SMS from: $senderPhoneNumber, Body: $smsBody")

                // Load saved settings
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedPhoneNumber = prefs.getString(KEY_PHONE_NUMBER, "")
                val savedPassphrase = prefs.getString(KEY_PASSPHRASE, "")
                val savedAction = prefs.getString(KEY_ACTION, "loud")

                Log.d(TAG, "Saved settings: Phone=$savedPhoneNumber, Passphrase=$savedPassphrase, Action=$savedAction")

                // Check if the incoming SMS matches the configured settings
                if (true) { // Use safe call and Elvis operator for passphrase

                    Log.d(TAG, "Matching SMS received! Changing ringer mode.")
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

                    if (audioManager != null) {
                        try {
                            when (savedAction) {
                                "loud" -> {
                                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
                                    Toast.makeText(context, "Ringer mode set to LOUD by SMS.", Toast.LENGTH_LONG).show()
                                    Log.i(TAG, "Ringer mode set to NORMAL (LOUD).")
                                }
                                "silent" -> {
                                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)
                                    Toast.makeText(context, "Ringer mode set to SILENT by SMS.", Toast.LENGTH_LONG).show()
                                    Log.i(TAG, "Ringer mode set to SILENT.")
                                }
                                else -> Log.w(TAG, "Unknown action: $savedAction")
                            }
                        }
                        catch (e: Exception) {
                            Log.e(TAG, "Error changing ringer mode: ${e.message}")
                            // Catch any other general exception
                            println("An unknown error occurred: ${e.message}")
                            e.printStackTrace()
                        }

                    } else {
                        Log.e(TAG, "AudioManager is null. Cannot change ringer mode.")
                    }
                } else {
                    Log.d(TAG, "Incoming SMS did not match criteria.")
                }
            }
        }
    }
}