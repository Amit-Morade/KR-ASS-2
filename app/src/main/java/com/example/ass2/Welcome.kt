package com.example.ass2
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.content.ContentValues

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ActivityDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ActivityLog.db"
        const val TABLE_NAME = "activity_log"
        const val COLUMN_ID = "id"
        const val COLUMN_START_TIME = "start_time"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_ACTIVITY = "activity"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableSQL = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_START_TIME TEXT, $COLUMN_DURATION INTEGER, $COLUMN_ACTIVITY TEXT)"
        db?.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertActivityLog(startTime: String, duration: Long, activity: String) {
        val values = ContentValues().apply {
            put(COLUMN_START_TIME, startTime)
            put(COLUMN_DURATION, duration)
            put(COLUMN_ACTIVITY, activity)
        }
        writableDatabase.insert(TABLE_NAME, null, values)
    }
}
@Composable
fun WelcomeScreen() {
    val currentTime = remember { mutableStateOf(Calendar.getInstance().time) }

    // Function to update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime.value = Calendar.getInstance().time
        }
    }

    // Greeting message
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        GreetingMessage(currentTime.value)
        ActivityTracker()
        DisplayActivityLogs(ActivityDatabaseHelper(LocalContext.current))

    }

}



// Constants for threshold values
private const val THRESHOLD_RUNNING = 5.0
private const val THRESHOLD_WALKING = 10.5
private const val THRESHOLD_VEHICLE = 20.0

@Composable
fun ActivityTracker() {
    val currentTime = remember { mutableStateOf(Calendar.getInstance().time) }
    val context = LocalContext.current
    val activity = remember { mutableStateOf("Still") }
    val activityStartTime = remember { mutableStateOf(System.currentTimeMillis()) }
    val lastActivityStartTime = remember { mutableStateOf(System.currentTimeMillis()) } // New state for last activity start time
    val activityDatabaseHelper = remember { ActivityDatabaseHelper(context) }
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.music)
    }

    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60)) % 24
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Initialize SensorManager
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Register Sensor Listener
    val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Example: Calculate magnitude of acceleration
                val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble())
                if (magnitude > THRESHOLD_VEHICLE){
                    if (activity.value != "Vehicle") {
                        lastActivityStartTime.value = System.currentTimeMillis() // Update last activity start time
                        activityStartTime.value = System.currentTimeMillis()
                        activityDatabaseHelper.insertActivityLog(currentTime.value.toString(), System.currentTimeMillis() - activityStartTime.value, "Vehicle")
                    }
                    activity.value = "Vehicle"
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                }
                // Example: Determine activity based on magnitude
                if (magnitude > THRESHOLD_RUNNING) {
                    if (activity.value != "Running") {
                        lastActivityStartTime.value = System.currentTimeMillis() // Update last activity start time
                        activityStartTime.value = System.currentTimeMillis()
                        activityDatabaseHelper.insertActivityLog(currentTime.value.toString(), System.currentTimeMillis() - activityStartTime.value, "Running")
                    }
                    activity.value = "Running"
                if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                    }

                } else if (magnitude > THRESHOLD_WALKING) {
                    if (activity.value != "Walking") {
                        lastActivityStartTime.value = System.currentTimeMillis() // Update last activity start time
                        activityStartTime.value = System.currentTimeMillis()
                        activityDatabaseHelper.insertActivityLog(currentTime.value.toString(), System.currentTimeMillis() - activityStartTime.value, "Walking")
                    }
                    activity.value = "Walking"
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                } else{
                    if (activity.value != "Still") {
                        lastActivityStartTime.value = System.currentTimeMillis() // Update last activity start time
                        activityStartTime.value = System.currentTimeMillis()
                        activityDatabaseHelper.insertActivityLog(currentTime.value.toString(), System.currentTimeMillis() - activityStartTime.value, "Still")
                    }
                    activity.value = "Still"
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }
                }
            }
        }
    }

    sensorManager.registerListener(
        accelerometerListener,
        accelerometerSensor,
        SensorManager.SENSOR_DELAY_NORMAL
    )

    // Composable UI
    Row {
        if (activity.value=="Still"){
        Image(painter = painterResource(id = R.drawable.still2), contentDescription = "Still")
        }
        else if (activity.value=="Walking"){
            Column {
                Image(painter = painterResource(id = R.drawable.walk), contentDescription = "Still")
                Text(text = "You can do It!! Try Running More Fast")
            }

        }
        else{
            Image(painter = painterResource(id = R.drawable.run), contentDescription = "Still")

        }
    }
    Column {
        Text(
            text = "Current Activity: ${activity.value}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        DisposableEffect(activity.value) {
            onDispose {
                val durationMillis = System.currentTimeMillis() - activityStartTime.value
                val durationText = formatDuration(durationMillis)
                Toast.makeText(context, "Duration of ${activity.value}: $durationText", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

@Composable
fun DisplayActivityLogs(databaseHelper: ActivityDatabaseHelper) {
    val context = LocalContext.current
    val activityLogs = remember { mutableStateOf(listOf<String>()) }

    // Query the database for activity logs
    LaunchedEffect(Unit) {
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${ActivityDatabaseHelper.TABLE_NAME}", null)
        val logs = mutableListOf<String>()

        if (cursor != null) {
            val startTimeIndex = cursor.getColumnIndex(ActivityDatabaseHelper.COLUMN_START_TIME)
            val durationIndex = cursor.getColumnIndex(ActivityDatabaseHelper.COLUMN_DURATION)
            val activityIndex = cursor.getColumnIndex(ActivityDatabaseHelper.COLUMN_ACTIVITY)

            while (cursor.moveToNext()) {
                val startTime = cursor.getString(startTimeIndex)
                val duration = cursor.getLong(durationIndex)
                val activity = cursor.getString(activityIndex)
                logs.add("Start Time: $startTime, Duration: $duration s, Activity: $activity")
            }
            cursor.close()
        }

        db.close()
        activityLogs.value = logs
    }

    // Display activity logs
    Column {
        Text(text = "Activity Logs", fontWeight = FontWeight.Bold)
        activityLogs.value.forEach {
            Text(text = it)
        }
    }
}

@Composable
fun GreetingMessage(currentTime: Date) {
    val formattedDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(currentTime)
    val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(currentTime)

    Text(
        text = "Hello! Current date and time:",
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
        text = "$formattedDate\n$formattedTime",
        textAlign = TextAlign.Center
    )
}
