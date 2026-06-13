package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.data.database.SatsWalkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PedometerManager(private val context: Context) : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private val db = SatsWalkDatabase.getDatabase(context)
    private val dao = db.satsWalkDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSensorRegistered = false
    private var baseSensorSteps = -1

    init {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        } catch (e: Exception) {
            Log.e("PedometerManager", "Error initializing sensor managers: ${e.localizedMessage}")
        }
    }

    fun startTracking() {
        if (isSensorRegistered) return
        stepCounterSensor?.let {
            try {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                isSensorRegistered = true
                Log.d("PedometerManager", "Pedometer hardware tracker registered successfully.")
            } catch (e: Exception) {
                Log.e("PedometerManager", "Failed to register sensor listener: ${e.localizedMessage}")
            }
        } ?: Log.w("PedometerManager", "No hardware step counter sensor found on this device.")
    }

    fun stopTracking() {
        if (!isSensorRegistered) return
        try {
            sensorManager?.unregisterListener(this)
            isSensorRegistered = false
            baseSensorSteps = -1
            Log.d("PedometerManager", "Pedometer hardware tracker unregistered.")
        } catch (e: Exception) {
            Log.e("PedometerManager", "Failed to unregister sensor: ${e.localizedMessage}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val currentPedometerValue = event.values[0].toInt()

        scope.launch {
            val user = dao.getUserProgress() ?: return@launch
            if (baseSensorSteps == -1) {
                baseSensorSteps = currentPedometerValue
            }

            val walkedDelta = currentPedometerValue - baseSensorSteps
            if (walkedDelta > 0) {
                val nextTotalSteps = user.currentSteps + walkedDelta
                baseSensorSteps = currentPedometerValue // step up base
                dao.insertUserProgress(user.copy(currentSteps = nextTotalSteps))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Dev/simulation injection to test all milestones on emulators
    fun addSimulatedSteps(stepsToAdd: Int, dateString: String) {
        scope.launch {
            val user = dao.getUserProgress()
            if (user != null) {
                val updatedSteps = user.currentSteps + stepsToAdd
                dao.insertUserProgress(
                    user.copy(
                        currentSteps = updatedSteps,
                        lastUpdatedDate = dateString
                    )
                )
            } else {
                // If not registered yet, preset
                dao.insertUserProgress(
                    com.example.data.database.UserProgressEntity(
                        deviceId = "emulator_device",
                        currentSteps = stepsToAdd,
                        lastUpdatedDate = dateString
                    )
                )
            }
        }
    }

    fun setSimulatedSteps(exactSteps: Int, dateString: String) {
        scope.launch {
            val user = dao.getUserProgress()
            if (user != null) {
                dao.insertUserProgress(
                    user.copy(
                        currentSteps = exactSteps,
                        lastUpdatedDate = dateString
                    )
                )
            } else {
                dao.insertUserProgress(
                    com.example.data.database.UserProgressEntity(
                        deviceId = "emulator_device",
                        currentSteps = exactSteps,
                        lastUpdatedDate = dateString
                    )
                )
            }
        }
    }
}
