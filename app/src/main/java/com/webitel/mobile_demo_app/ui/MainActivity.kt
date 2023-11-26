package com.webitel.mobile_demo_app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.databinding.ActivityMainBinding
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.VoiceClient

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val notifications = Notifications.instance


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        checkAndRequestPermissions()
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_HANGUP_CALL_FROM_NOTIFY" -> {
                DemoApp.instance.portalClient.getVoiceClient(object :
                    CallbackListener<VoiceClient> {
                    override fun onSuccess(t: VoiceClient) {
                        hangupCall(t)
                    }

                    override fun onError(e: Error) {
                        Toast.makeText(
                            this@MainActivity,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
            }
            "OPEN_CALL_DETAIL_FROM_NOTIFY" -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                if (navController.currentDestination?.id == R.id.FirstFragment) {
                    navController.navigate(R.id.action_FirstFragment_to_callDetailFragment)
                }
            }
        }
        intent?.action = null
    }


    private fun hangupCall(t: VoiceClient) {
        t.activeCall?.disconnect()
    }


    private fun checkAndRequestPermissions(): Boolean {
        val recordAudio =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)


        val listPermissionsNeeded: MutableList<String> = ArrayList()

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                1
            )
            return false
        }
        return true
    }
}