package com.webitel.mobile_demo_app.chat

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.databinding.FragmentChatBinding
import kotlinx.coroutines.launch


class ChatFragment : Fragment(R.layout.fragment_chat) {
    private var binding: FragmentChatBinding? = null
    private val vm: ChatViewModel by activityViewModels()
    private var lastSize = 0
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
                onDownloadFileClick(it.message!!.id)
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
                vm.sendMessage(t.toString())
                binding?.inputMessage?.setText("")
            }
        }

        binding?.chooseFileBtn?.setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.messages.collect { list ->
                    val rv = binding?.rv ?: return@collect
                    val isNewItem = list.size > lastSize

                    val shouldScroll = (isNewItem && (isNearBottom(rv) || !list.lastOrNull()?.requestId.isNullOrEmpty()))
                    adapter.submitList(list) {
                        if (shouldScroll && list.isNotEmpty()) {
                            rv.scrollToPosition(list.size - 1)
                        }
                    }

                    lastSize = list.size
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.errorEvents.collect { err ->
                    Snackbar.make(
                        binding!!.root,
                        err,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun isNearBottom(rv: RecyclerView, threshold: Int = 2): Boolean {
        val lm = rv.layoutManager as? LinearLayoutManager ?: return true
        val lastVisible = lm.findLastVisibleItemPosition()
        val total = lm.itemCount
        return lastVisible >= total - 1 - threshold
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
        vm.sendMediaMessage(uri)
    }


    private fun onDownloadFileClick(messageId: Long) {
        if (messageId > 0)
          //  vm.downloadFile(messageId)
        else {
            Toast.makeText(
                requireContext(),
                "messageId is not valid",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}