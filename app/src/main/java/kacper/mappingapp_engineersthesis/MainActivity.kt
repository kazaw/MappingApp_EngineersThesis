package kacper.mappingapp_engineersthesis

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import android.content.Context.ACTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.app.ActivityManager
import androidx.core.app.ComponentActivity.ExtraData
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.R.attr.name
import android.os.Handler
import java.lang.IllegalArgumentException


class MainActivity : AppCompatActivity() {
    private var filename: String = ""

    fun returnDirectionCharacter(rotX: Int) : String{
        return when (rotX) {
            in -22..22 -> "N"
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..180 -> "S"
            in -180..-158 -> "S"
            in -157..-113 -> "SW"
            in -112..-68 -> "W"
            in -67..-22 -> "NW"
            else -> throw IllegalArgumentException("Wrong Number in direction")
        }
    }

    private val timerUIUpdated = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            findViewById<TextView>(R.id.textView_timer).apply {
                text = intent.extras!!.getString("time")
            }
            filename = intent.extras!!.getString("filename")!!
            //-----------------------------------------------------------
/*            findViewById<TextView>(R.id.textViewAccx).apply {
                text = intent.extras!!.getString("AccX")
            }
            findViewById<TextView>(R.id.textViewAccy).apply {
                text = intent.extras!!.getString("AccY")
            }
            findViewById<TextView>(R.id.textViewAccz).apply {
                text = intent.extras!!.getString("AccZ")
            }*/
            //----
            findViewById<TextView>(R.id.textView_state).apply {
                text = intent.extras!!.getString("RotX") + " " + returnDirectionCharacter(intent.extras!!.getString("RotX")?.toInt()!!)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_settings -> {
                startSettings()
                true
            }
            R.id.menu_help -> {
                showHelp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    fun startSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {}
        val startingDelay: Long = 5000
        intent.putExtra("startingDelay", startingDelay)
        startActivity(intent)

    }

    fun showHelp() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        findViewById<TextView>(R.id.textView_timer).apply {
            text = sharedPreferences.getString("diameter", "")//działa
        }

    }

    fun button_start_onClick(view: View){
        val button = findViewById<Button>(R.id.button_start)
        val imageView = findViewById<ImageView>(R.id.imageView1)
        intent = Intent(this, MappingService::class.java)
        if (button.text == "stop") {
            this.stopService(intent)
            Handler().postDelayed(
                {
                    val drawingClass = DrawingClass(this,filename)
                    var bitmap = drawingClass.returnBitmap()
                    bitmap = drawingClass.measureDimensions(bitmap)
                    imageView.setImageBitmap(bitmap)
                    drawingClass.saveImageToFile(bitmap)
                },
                1000) // value in milliseconds

            button.text = "start"
        } else {
            button.text = "stop"
            this.startService(intent)
            this.registerReceiver(timerUIUpdated, IntentFilter("MAPPER_UPDATED"))

        }

    }



}
