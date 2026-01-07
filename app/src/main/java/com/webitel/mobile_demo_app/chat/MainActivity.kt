package com.webitel.mobile_demo_app.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.webitel.mobile_demo_app.databinding.ActivityMainBinding
import com.webitel.mobile_sdk.domain.ConnectListener
import com.webitel.mobile_sdk.domain.ConnectState
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), ConnectListener, SettingDialogFragment.OnConnectionDataEntered {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.title = ""
        binding.configButton.setOnClickListener {
            showConnectionDialog()
        }

        lifecycleScope.launch {
            lifecycleScope.launchWhenStarted {
                viewModel.connectState.collect { state ->
                    setConnectStateUI(state)
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }


    override fun onStateChanged(from: ConnectState, to: ConnectState) {
        runOnUiThread {
            setConnectStateUI(to)
        }
    }


    override fun onDataEntered(auth: AuthInfo) {
        viewModel.updateAuthInfo(auth)
    }


    private fun showConnectionDialog() {
        val dialog = viewModel.getAuthInfo()
            ?.let { SettingDialogFragment.newInstance(it) }
            ?: SettingDialogFragment()
        dialog.show(supportFragmentManager, "SettingDialog")
    }


    private fun setConnectStateUI(state: ConnectState) {
        when(state) {
            is ConnectState.Connecting -> {
                binding.toolbarTitle.text = "Connecting..."
            }
            is ConnectState.Ready -> {
                binding.toolbarTitle.text = "\uD83D\uDFE2 Ready"
            }
            is ConnectState.Disconnected -> {
                binding.toolbarTitle.text = "\uD83D\uDD34 Disconnected"
            }
            is ConnectState.None -> {
                binding.toolbarTitle.text = "⚪"
            }
        }
    }
}