package com.example.todolistapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Alarm ringing!", Toast.LENGTH_SHORT).show()

        // Play the alarm sound
        val mediaPlayer = MediaPlayer.create(context, R.raw.alarm)
        mediaPlayer.start()

        // Release the MediaPlayer resources when the sound is completed
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }
}
