package com.webitel.mobile_demo_app.ui.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.webitel.mobile_demo_app.databinding.FragmentChatBinding


class ChatFragment : Fragment() {
    private var binding: FragmentChatBinding? = null
    private val vm: ChatViewModel by activityViewModels()
    private lateinit var adapter: ChatAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChatAdapter(vm.messages.value!!)
        binding?.rv?.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding?.rv?.adapter = adapter
        binding?.rv?.setItemViewCacheSize(100)
        binding?.layoutSend?.setOnClickListener {
            val t = binding?.inputMessage?.text
            if (t != null) {
                vm.sendMessage(t.toString())
                binding?.inputMessage?.setText("")
            }
        }

        vm.messages.observe(viewLifecycleOwner) {
            adapter.setMessages(it)
            adapter.notifyDataSetChanged()
            binding?.rv?.scrollToPosition(vm.messages.value!!.size - 1)
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
        vm.getChatClient()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding!!.root
    }


    override fun onResume() {
        vm.subscribeListener()
        vm.getUpdates()
        vm.setActiveDialog(true)
        super.onResume()
    }


    override fun onPause() {
        vm.setActiveDialog(false)
        super.onPause()
    }
}