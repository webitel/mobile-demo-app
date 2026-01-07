package com.webitel.mobile_demo_app.chat

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.webitel.mobile_demo_app.R


/**
 * DialogFragment to input connection settings like host, token, issuer, and username.
 * Sends the result back via [OnConnectionDataEntered].
 */
class SettingDialogFragment : DialogFragment() {
    private var listener: OnConnectionDataEntered? = null

    /**
     * Interface used to return entered connection data to the host (Activity).
     */
    interface OnConnectionDataEntered {
        fun onDataEntered(auth: AuthInfo)
    }


    companion object {
        fun newInstance(auth: AuthInfo): SettingDialogFragment {
            val fragment = SettingDialogFragment()
            val args = Bundle().apply {
                putString("host", auth.host)
                putString("token", auth.token)
                putString("issuer", auth.issuer)
                putString("userName", auth.userName)
                putBoolean("isWebSocket", auth.isWebSocket)
            }
            fragment.arguments = args
            return fragment
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnConnectionDataEntered) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnConnectionDataEntered")
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.fragment_setting_dialog, null)

        val hostField = view.findViewById<EditText>(R.id.editHost)
        val tokenField = view.findViewById<EditText>(R.id.editToken)
        val issuerField = view.findViewById<EditText>(R.id.editIssuer)
        val userNameField = view.findViewById<EditText>(R.id.editUserName)
        val isWebSocketField = view.findViewById<SwitchMaterial>(R.id.isWebSocketSwitch)

        val defaultHost = arguments?.getString("host") ?: ""
        val defaultToken = arguments?.getString("token") ?: ""
        val defaultIssuer = arguments?.getString("issuer") ?: ""
        val defaultUserName = arguments?.getString("userName") ?: ""
        val defaultIsWebSocket = arguments?.getBoolean("isWebSocket") ?: false

        hostField.setText(defaultHost)
        tokenField.setText(defaultToken)
        issuerField.setText(defaultIssuer)
        userNameField.setText(defaultUserName)
        isWebSocketField.isChecked = defaultIsWebSocket

        builder.setView(view)
            .setTitle("Connect to Service")
            .setPositiveButton("Save") { _, _ ->
                val host = hostField.text.toString()
                val token = tokenField.text.toString()
                val issuer = issuerField.text.toString()
                val userName = userNameField.text.toString()
                val isWebSocket = isWebSocketField.isChecked

                listener?.onDataEntered(
                    AuthInfo(host,token,issuer,userName, isWebSocket)
                )
            }
            .setNegativeButton("Cancel", null)
        return builder.create()
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}