package com.example.todolistapp

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var tasks: MutableList<Task>
    private lateinit var emptyView: View
    private lateinit var fab: FloatingActionButton
    private lateinit var mediaPlayer: MediaPlayer

    companion object {
        const val CHANNEL_ID = "todo_list_channel"
        const val NOTIFICATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        checkNotificationPermission()

        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.empty_view)
        fab = findViewById(R.id.fab)

        recyclerView.layoutManager = LinearLayoutManager(this)
        tasks = mutableListOf()
        taskAdapter = TaskAdapter(tasks, this)
        recyclerView.adapter = taskAdapter

        fab.setOnClickListener {
            showAddTaskDialog()
        }

        // Show empty view if the task list is empty
        if (tasks.isEmpty()) {
            showEmptyView()
        } else {
            hideEmptyView()
        }

        // Initialize the MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.notification)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
            }
        }
    }

    private fun showAddTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Task")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogView.findViewById<EditText>(R.id.editTextTitle).text.toString()
                val description = dialogView.findViewById<EditText>(R.id.editTextDescription).text.toString()
                val day = selectedDay
                val date = selectedDate
                val month = selectedMonth
                val year = selectedYear.toIntOrNull() ?: 0 // Convert to Int or default to 0
                val time = selectedTime
                val task = Task(day, date, month, year, time, title, description)
                tasks.add(task)
                taskAdapter.notifyItemInserted(tasks.size - 1)
                hideEmptyView()
                showNotification(title, description)
            }
        val dialog = builder.create()
        dialog.show()

        val buttonSetDate = dialogView.findViewById<Button>(R.id.buttonSetDate)
        buttonSetDate.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDay = getDayOfWeek(dayOfMonth, month, year)
                selectedDate = dayOfMonth.toString()
                selectedMonth = getMonthName(month)
                selectedYear = year.toString()
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        val buttonSetTime = dialogView.findViewById<Button>(R.id.buttonSetTime)
        buttonSetTime.setOnClickListener {
            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedTime = "$hourOfDay:$minute"
            }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true)
            timePicker.show()
        }

        val buttonSetAlarm = dialogView.findViewById<Button>(R.id.buttonSetAlarm)
        buttonSetAlarm.setOnClickListener {
            setAlarm()
        }
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedTime.split(":")[0].toInt())
            set(Calendar.MINUTE, selectedTime.split(":")[1].toInt())
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please enable exact alarms for this app in settings.", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            Toast.makeText(this, "Alarm set for $selectedTime", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Unable to set exact alarm. Please enable exact alarms in settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getDayOfWeek(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "Day"
    }

    private fun getMonthName(month: Int): String {
        return Calendar.getInstance().apply { set(Calendar.MONTH, month) }
            .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "Month"
    }

    private var selectedDay: String = ""
    private var selectedDate: String = ""
    private var selectedMonth: String = ""
    private var selectedYear: String = ""
    private var selectedTime: String = ""

    override fun onItemClick(position: Int) {
        // Handle item click (Edit or Delete task)
        showEditTaskDialog(position)
    }

    override fun onEditClick(position: Int) {
        // Handle edit click
        showEditTaskDialog(position)
    }

    override fun onDeleteClick(position: Int) {
        // Handle delete click
        showDeleteTaskDialog(position)
    }

    private fun showEditTaskDialog(position: Int) {
        val task = tasks[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        editTextTitle.setText(task.title)
        editTextDescription.setText(task.description)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Task")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update") { _, _ ->
                task.title = editTextTitle.text.toString()
                task.description = editTextDescription.text.toString()
                taskAdapter.notifyItemChanged(position)
            }
        val dialog = builder.create()
        dialog.show()

        val buttonSetDate = dialogView.findViewById<Button>(R.id.buttonSetDate)
        buttonSetDate.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDay = getDayOfWeek(dayOfMonth, month, year)
                selectedDate = dayOfMonth.toString()
                selectedMonth = getMonthName(month)
                selectedYear = year.toString()
                task.day = selectedDay
                task.date = selectedDate
                task.month = selectedMonth
                task.year = selectedYear.toIntOrNull() ?: 0 // Convert to Int or default to 0
                task.time = selectedTime
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        val buttonSetTime = dialogView.findViewById<Button>(R.id.buttonSetTime)
        buttonSetTime.setOnClickListener {
            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedTime = "$hourOfDay:$minute"
                task.time = selectedTime
            }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY), Calendar.getInstance().get(Calendar.MINUTE), true)
            timePicker.show()
        }

        val buttonSetAlarm = dialogView.findViewById<Button>(R.id.buttonSetAlarm)
        buttonSetAlarm.setOnClickListener {
            setAlarm()
        }
    }

    private fun showDeleteTaskDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                tasks.removeAt(position)
                taskAdapter.notifyItemRemoved(position)
                if (tasks.isEmpty()) {
                    showEmptyView()
                }
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showEmptyView() {
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyView() {
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ToDoList Channel"
            val descriptionText = "Channel for ToDoList notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("MainActivity", "Notification channel created")
        }
    }

    private fun showNotification(title: String, description: String) {
        val soundUri = Uri.parse("android.resource://${packageName}/raw/notification")
        Log.d("MainActivity", "Sound URI: $soundUri")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        Log.d("MainActivity", "Showing notification with title: $title and description: $description")
        NotificationManagerCompat.from(this).notify(1, notification)

        // Play the notification sound
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer resources when the activity is destroyed
        mediaPlayer.release()
    }
}
