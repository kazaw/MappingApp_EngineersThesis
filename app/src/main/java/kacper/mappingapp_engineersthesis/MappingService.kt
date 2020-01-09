package kacper.mappingapp_engineersthesis

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import java.lang.Math.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import java.io.File
import java.util.*
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Environment
import java.io.BufferedWriter
import java.io.FileWriter
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import java.text.SimpleDateFormat
import androidx.core.app.NotificationCompat.getExtras
import android.os.Bundle
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.NotificationCompat.getExtras
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.preference.PreferenceManager
import android.os.CountDownTimer
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class MappingService : Service(), SensorEventListener {
    //<---------------------------------------------- Sensors ---------------------------------------------->
    private var linearAcceleration = FloatArray(3)
    private var gravity = FloatArray(3)
    private var rMatrix = FloatArray(9)
    private var rotation = FloatArray(3)
    private var isCountdownRunning = false
    override fun onSensorChanged(event: SensorEvent?) {
        val sensor = event?.sensor
        if (sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val alpha = 0.8f

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

            // Remove the gravity contribution with the high-pass filter.
            linearAcceleration[0] = event.values[0] - gravity[0]
            linearAcceleration[1] = event.values[1] - gravity[1]
            linearAcceleration[2] = event.values[2] - gravity[2]

        }
        if (sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION){
            linearAcceleration[0] = event.values[0]
            linearAcceleration[1] = event.values[1]
            linearAcceleration[2] = event.values[2]
        }
        if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR){
            rotation = calculateAngles(rotation,event.values)
            //rotation[0] = event.values[0]//roll
            //rotation[1] = event.values[1]//pitch
            //rotation[2] = event.values[2]//jaw
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    fun calculateAngles(result: FloatArray, rVector: FloatArray): FloatArray {
        //calculate rotation matrix from rotation vector first
        SensorManager.getRotationMatrixFromVector(rMatrix, rVector)

        //calculate Euler angles now
        SensorManager.getOrientation(rMatrix, result)

        //The results are in radians, need to convert it to degrees
        return convertToDegrees(result)
    }

    private fun convertToDegrees(vector: FloatArray): FloatArray {
        for (i in vector.indices) {
            vector[i] = toDegrees(vector[i].toDouble()).roundToInt().toFloat()
        }
        return vector
    }

    //<---------------------------------------------- Service ---------------------------------------------->
    private lateinit var filename: String
    private lateinit var file: File
    private lateinit var bufferedWriter: BufferedWriter
    private lateinit var countDownTimer: CountDownTimer
    private val csvHeader = "time;AccX;AccY;AccZ;RotX"
    var startTime: Long = 0
    var delayMillis: Long = 100
    var rotationThreshold = 5
    var lastRotation: Float = 1000f
    var mappingHandler = Handler()
    var mappingRunnable: Runnable = object : Runnable {

        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            val seconds = millis / 1000
            val time = String.format("%02d:%02d:%02d:%02d", (seconds / 3600) ,(seconds % 3600) / 60, (seconds % 60), ((millis % 1000 )/ 10))
            val intent = Intent("MAPPER_UPDATED")
            intent.putExtra("time", time)
            intent.putExtra("filename", filename)
            intent.putExtra("AccX", String.format("%.2f",linearAcceleration[0]))
            intent.putExtra("AccY", String.format("%.2f",linearAcceleration[1]))
            intent.putExtra("AccZ", String.format("%.2f",linearAcceleration[2]))
            intent.putExtra("RotX", String.format("%d",rotation[0].toInt()))
            if (kotlin.math.abs(lastRotation - rotation[0]) < rotationThreshold){
                if (!isCountdownRunning) countDownTimer.start()
            }else {
                isCountdownRunning = false
                countDownTimer.cancel()
            }
            lastRotation = rotation[0]
            sendBroadcast(intent)
            bufferedWriter.appendln(String.format("%d;%.2f;%.2f;%.2f;%d",
                millis, linearAcceleration[0], linearAcceleration[1], linearAcceleration[2],rotation[0].toInt()))
            mappingHandler.postDelayed(this, delayMillis)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        //val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        //val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL)
        //sensorManager.registerListener(this,gyroscope,SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,rotationVector,SensorManager.SENSOR_DELAY_NORMAL)
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
        //val date = Calendar.getInstance().time.toString()
        val date = sdf.format(Calendar.getInstance().time)
        filename = "MappingApp_" + date + ".csv"
        file = File(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)
        //file.mkdir()
        bufferedWriter = BufferedWriter(FileWriter(file))
        bufferedWriter.write(csvHeader)
        bufferedWriter.append("\n")
        val startingDelay: Long = sharedPreferences.getString("startingDelay", "")!!.toLong()
        delayMillis = sharedPreferences.getString("samplingFrequency", "")!!.toLong()
        val inactivityTimeLimit: Long = sharedPreferences.getString("inactivityTimeLimit", "")!!.toLong()
        startTime = System.currentTimeMillis() + startingDelay
        countDownTimer = object: CountDownTimer(inactivityTimeLimit, delayMillis/2){
            override fun onFinish() {
                stopSelf()
            }

            override fun onTick(millisUntilFinished: Long) {
                isCountdownRunning = true
            }

        }
        mappingHandler.postDelayed(mappingRunnable, startingDelay)
        return START_STICKY
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        mappingHandler.removeCallbacks(mappingRunnable)
        bufferedWriter.close()
        super.onDestroy()
    }
}
