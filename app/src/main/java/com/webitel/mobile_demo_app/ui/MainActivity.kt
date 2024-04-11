package com.webitel.mobile_demo_app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.databinding.ActivityMainBinding
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.ConnectListener
import com.webitel.mobile_sdk.domain.ConnectState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity(), ConnectListener {

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

        FirebaseMessaging.getInstance().token.addOnSuccessListener { s ->

            runBlocking {
                try {
                    DemoApp.instance.portalClient2.registerFCMToken(s)
                } catch (e: Exception) {
                    Log.e("sendRegistration", e.message.toString())
                }
            }

        }.addOnFailureListener {
            Toast.makeText(
                this@MainActivity,
                "Failed to get token",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onResume() {
        super.onResume()
        DemoApp.instance.portalClient2.addConnectListener(this)
        val state = DemoApp.instance.portalClient2.getConnectState()
        setConnectStateUI(state)
    }


    override fun onPause() {
        DemoApp.instance.portalClient2.addConnectListener(this)
        super.onPause()
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


    override fun onStateChanged(from: ConnectState, to: ConnectState) {
        Log.e("onStateChanged", "from - ${from.name}, to - ${to.name}")
        runOnUiThread {
            setConnectStateUI(to)
        }
    }


    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_HANGUP_CALL_FROM_NOTIFY" -> {
                hangupCall()
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


    private fun hangupCall() {
        lifecycleScope.launch(IO) {
            try {
                DemoApp.instance.portalClient2.disconnectCall()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun checkAndRequestPermissions(): Boolean {
        val recordAudio =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val readExternal =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)


        val listPermissionsNeeded: MutableList<String> = ArrayList()

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (readExternal != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
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


    private fun setConnectStateUI(state: ConnectState) {
        when(state) {
            ConnectState.CONNECTING -> {
                binding.toolbarTitle.text = "Connecting..."
            }
            ConnectState.READY -> {
                binding.toolbarTitle.text = "\uD83D\uDFE2 Ready"
            }
            ConnectState.DISCONNECTED -> {
                binding.toolbarTitle.text = "\uD83D\uDD34 Disconnected"
                Log.e("DISCONNECTED", ConnectState.DISCONNECTED.message)
            }
        }
    }
}