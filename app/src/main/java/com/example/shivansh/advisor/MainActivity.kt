package com.example.shivansh.advisor
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ExecutionException

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var senSensorManager: SensorManager? = null
    private var senAccelerometer: Sensor? = null
    private var result: String? = null
    private var count = 0
    private var lastUpdate: Long = 0
    private var last_x: Float = 0.toFloat()
    private var last_y: Float = 0.toFloat()
    private var last_z: Float = 0.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        senSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        senAccelerometer = senSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        senSensorManager!!.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        message = findViewById(R.id.advice)

    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val mySensor = sensorEvent.sensor

        if (mySensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]
            val curTime = System.currentTimeMillis()

            if (curTime - lastUpdate > 200) {
                val diffTime = curTime - lastUpdate
                lastUpdate = curTime
                val speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000
                if (speed > SHAKE_THRESHOLD) {
                    count++
                    if (count % 2 != 0) {
                        if (call) {
                            call = false
                            val senddetails = getComplaints()
                            try {
                                result = senddetails.execute().get()
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            } catch (e: ExecutionException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
                last_x = x
                last_y = y
                last_z = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {

    }

    override fun onPause() {
        super.onPause()
        senSensorManager!!.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        senSensorManager!!.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    internal class getComplaints : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg voids: Void): String? {

            var result: String? = null
            val base_url = "https://api.adviceslip.com/advice"

            try {
                val myUrl = URL(base_url)
                result = URL(myUrl.toString()).readText()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return result
        }
        override fun onPostExecute(result: String) {

            val jsonObject: JSONObject
            try {
                jsonObject = JSONObject(result)
                val full = jsonObject.getString("slip")
                val advice = full.substring(10, full.indexOf("slip_id") - 2)
                Log.e("hmm", advice)
                message!!.text = advice
                call = true

            } catch (e: JSONException) {
                Log.e("log", e.localizedMessage)
            }

            super.onPostExecute(result)
        }
    }

    companion object {
        private var call = true
        private var message: TextView? = null
        private val SHAKE_THRESHOLD = 1200
    }
}
