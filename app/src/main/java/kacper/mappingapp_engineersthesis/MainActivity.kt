package kacper.mappingapp_engineersthesis

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager






class MainActivity : AppCompatActivity() {

    private val timerUIUpdated = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            findViewById<TextView>(R.id.textView_timer).apply {
                text = intent.extras!!.getString("time")
            }
            //-----------------------------------------------------------
            findViewById<TextView>(R.id.textViewAccx).apply {
                text = intent.extras!!.getString("AccX")
            }
            findViewById<TextView>(R.id.textViewAccy).apply {
                text = intent.extras!!.getString("AccY")
            }
            findViewById<TextView>(R.id.textViewAccz).apply {
                text = intent.extras!!.getString("AccZ")
            }
            //----
            findViewById<TextView>(R.id.textViewRotX).apply {
                text = intent.extras!!.getString("RotX")
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
        intent = Intent(this, MappingService::class.java)
        if (button.text == "stop") {
            this.stopService(intent)
            button.text = "start"
        } else {
            button.text = "stop"
            this.startService(intent)
            this.registerReceiver(timerUIUpdated, IntentFilter("MAPPER_UPDATED"))
        }

    }


}
