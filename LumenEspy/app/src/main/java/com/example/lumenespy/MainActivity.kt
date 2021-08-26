package com.example.lumenespy

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.LinkedList
import kotlin.math.ceil
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(),SensorEventListener {

    private lateinit var sensorManager:SensorManager
    private var brightness: Sensor? = null
    private lateinit var textLX: TextView
    private lateinit var textLevel :TextView
    private lateinit var textMin: TextView
    private lateinit var textMax: TextView
    private lateinit var textAvg: TextView
    private lateinit var pb: CircularProgressBar
    private var ilMax = 0f
    private var ilMin = 1000f
    private var ilAvg = 0f
    private lateinit var ilQ: Array<Any>
    private var qqlist = LinkedList<Int>(listOf())
    private lateinit var aaChartView: AAChartView
    private lateinit var aaChartModel: AAChartModel
    private lateinit var refresh: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        refresh = findViewById(R.id.btnRefresh)
        textLX = findViewById(R.id.textIllu)
        textLevel = findViewById(R.id.textLevel)
        textMin = findViewById(R.id.textMin)
        textMax = findViewById(R.id.textMax)
        textAvg = findViewById(R.id.textAvg)
        pb = findViewById(R.id.circularProgressBar)
        pb.apply{
            // progress max
            progressMax = 500f

            // Set ProgressBar Color with gradient
            progressBarColorStart = Color.CYAN
            progressBarColorEnd = Color.YELLOW
            progressBarColorDirection = CircularProgressBar.GradientDirection.TOP_TO_BOTTOM

            // Set background ProgressBar Color
            backgroundProgressBarColor = Color.GRAY


            // Set Width
            progressBarWidth = 22f // in DP
            backgroundProgressBarWidth = 14f // in DP

            // Other
            roundBorder = true

            progressDirection = CircularProgressBar.ProgressDirection.TO_RIGHT
        }
        // initialize array for graph
        ilQ = arrayOf(0f)

        // initialize graph
        aaChartView = findViewById(R.id.aa_chart_view)
        aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .dataLabelsEnabled(true)
            .series(arrayOf(
                AASeriesElement()
                    .color("CYAN")
                    .name("Light(lx)")
                    .data(ilQ)
            )
            )
        // draw graph
        aaChartView.aa_drawChartWithChartModel(aaChartModel)

        // refresh button to reset everything
        refresh.setOnClickListener{
            // reset progressbar
            textLX.text = "0"
            textLevel.setText("0")
            pb.setProgressWithAnimation(0f)

            // reset graph
            qqlist = LinkedList<Int>(listOf())
            ilQ = arrayOf(0f)
            aaChartView.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(
                AASeriesElement()
                    .color("CYAN")
                    .name("Light(lx)")
                    .data(ilQ)))

            //reset min max
            ilAvg = 0f
            ilMin = 1000f
            ilMax = 0f
            textMin.text = "0"
            textMax.text = "0"
            textAvg.text = "0"

            // give some delay before resume sensor
            onPause()
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({onResume()},500)


        }

        setupSensor()
    }

    private fun setupSensor(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        brightness = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    }

    // brightness ranking
    private fun brightness(brightness:Float): String{
        return when (brightness.toInt()){
            0 -> "Too dark"
            in 1..50 -> "Dark"
            in 51..200 -> "Dim"
            in 201..2000 -> "Perfect lighting"
            in 2001..5000 -> "Bright"
            in 5001..10000 -> "Very bright"
            else -> "WARNING: \n This may BLIND YOU!!!"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type==Sensor.TYPE_LIGHT){
            val light = event.values[0]

            // set progress bar and illuminance
            textLX.text = "${light.roundToInt()} lx"
            textLevel.setText("${brightness(light)}")
            pb.setProgressWithAnimation(light)


            // for updating the queue
            qqlist.add(light.roundToInt())
            if (qqlist.size >= 10){
                qqlist.remove()
            }
            ilQ = qqlist.toArray()

            // progress bar ranking
            if (light>2000f && light < 10000f) {
                pb.progressMax = 10000f
                pb.progressBarColorStart = Color.YELLOW
                pb.progressBarColorEnd = Color.RED
            }
            else if (light > 10000f){
                pb.progressBarColorStart = Color.RED
                pb.progressBarColorEnd = Color.RED
            }else{
                pb.progressMax = 500f
                pb.progressBarColorStart = Color.CYAN
                pb.progressBarColorEnd = Color.YELLOW
            }

            //update graph
            aaChartView.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(
                AASeriesElement()
                    .color("CYAN")
                    .name("Light(lx)")
                    .data(ilQ)))

            // to update min max and average
            if (light > ilMax){
                ilMax = light
                textMax.text = "${ilMax.roundToInt()}"
            }

            if (light < ilMin){
                ilMin = light
                textMin.text = "${ilMin.roundToInt()}"
            }

            ilAvg = ilQ.average()
            textAvg.text = "${ilAvg.roundToInt()}"

        }
    }

    private fun Array<out Any>.average():Float{
        var sum: Float = 0f
        var count: Int = 0
        for (element in this) {
            sum += element.toString().toFloat()
            ++count
        }
        return if (count == 0) Float.NaN else ceil(sum / count)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this,brightness,SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}