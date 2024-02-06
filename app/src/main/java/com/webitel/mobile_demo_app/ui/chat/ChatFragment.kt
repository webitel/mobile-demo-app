package com.webitel.mobile_demo_app.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.webitel.mobile_demo_app.databinding.FragmentChatBinding


class ChatFragment : Fragment() {
    private var binding: FragmentChatBinding? = null
    private val vm: ChatViewModel by activityViewModels()
    private lateinit var adapter: ChatAdapter
    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                sendMediaMessage(it)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding!!.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatAdapter(
            onDownloadFile = {
                onDownloadFileClick(it.id)
            },
            onOpenMedia = { mimeType, uri ->
                if (mimeType.startsWith("image/")) {
                    viewUri(uri)
                } else {
                    shareUri(uri, mimeType)
                }
            }
        )

        val llm = MessagesLinearLayoutManager(
            requireContext()
        )
        llm.stackFromEnd = true

        binding?.rv?.layoutManager = llm
        binding?.rv?.adapter = adapter
        binding?.rv?.setItemViewCacheSize(100)
        binding?.layoutSend?.setOnClickListener {
            val t = binding?.inputMessage?.text
            if (t != null) {
                vm.sendMessage(t.toString(), null)
                binding?.inputMessage?.setText("")
            }
        }

        binding?.chooseFileBtn?.setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }

        vm.messages.observe(viewLifecycleOwner) {
            val newest = adapter.getNewestMessage()
            adapter.submitList(it ?: listOf())
            if (it != null && it.lastOrNull()?.uuid != newest?.uuid) {
                binding?.rv?.smoothScrollToPosition(it.size - 1)
            }
        }

        vm.error.observe(viewLifecycleOwner) {
            if (it != null) {
                binding?.errorText?.text = "${it.code}\n${it.message}"
                binding?.errorText?.visibility = View.VISIBLE
                binding?.inputMessage?.isEnabled = false
                binding?.layoutSend?.isEnabled = false
            } else {
                binding?.inputMessage?.isEnabled = true
                binding?.layoutSend?.isEnabled = true
                binding?.errorText?.visibility = View.GONE
            }
        }
    }


    override fun onResume() {
        vm.getUpdates()
        super.onResume()
    }


    private fun viewUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, null))
    }


    private fun shareUri(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, null))
    }


    private fun sendMediaMessage(uri: Uri) {
        val t = binding?.inputMessage?.text ?: ""
        vm.sendMessage(
            text = t.toString(),
            uri
        )
    }


    private fun onDownloadFileClick(messageId: Long?) {
        if (messageId != null && messageId > 0)
            vm.downloadFile(messageId)
        else {
            Toast.makeText(
                requireContext(),
                "messageId is not valid",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}