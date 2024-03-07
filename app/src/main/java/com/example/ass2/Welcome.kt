package com.example.ass2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        RecognizeActivity()
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

@Composable
fun RecognizeActivity() {
    val sensorManager = LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val sensorListener = remember { MySensorListener() }

    // Start listening to accelerometer sensor events
    LaunchedEffect(Unit) {
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    // Display recognized activity
    Text(
        text = "Recognized activity: ${sensorListener.currentActivity}",

        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 32.dp)
    )

    // Cleanup: unregister sensor listener when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

}

class MySensorListener : SensorEventListener {
    var currentActivity by mutableStateOf("Unknown")

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this example
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Perform your activity recognition logic here based on accelerometer data
            // For demonstration, we'll just check if the device is moving or not
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (acceleration > 0.5f) {
                currentActivity = "Moving"
            } else {
                currentActivity = "Still"
            }
        }
    }
}