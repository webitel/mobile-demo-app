package com.webitel.mobile_demo_app.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.webitel.mobile_demo_app.ui.FragmentViewModel.UserActivity
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.databinding.FragmentSimpleBinding


class SimpleFragment : Fragment() {
    private var _binding: FragmentSimpleBinding? = null
    private var isFABOpen = false

    private val binding get() = _binding!!
    private val vm: FragmentViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fab.setOnClickListener {
            onCallChatButtonClick()
        }

        binding.fabCall.setOnClickListener {
            closeFABMenu()
            openCallDetail()
        }

        binding.fabChat.setOnClickListener {
            closeFABMenu()
            openChatDetail()
        }

        vm.userActivity.observe(viewLifecycleOwner) {
            if (it != null) {
                setupButton(it)
            }
        }

        vm.isLoading.observe(viewLifecycleOwner) {
            if (it != null) {
                setupProgressBar(it)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        vm.checkActivities()
    }

    override fun onPause() {
        super.onPause()
        closeFABMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupProgressBar(visible: Boolean) {
        binding.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setupButton(userActivity: UserActivity) {
        when (userActivity) {
            UserActivity.CALL -> {
                binding.fab.setImageResource(R.drawable.ic_call)
                binding.fab.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.negative)
            }

            UserActivity.NONE -> {
                binding.fab.setImageResource(R.drawable.ic_call_chat)
                binding.fab.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.accent)
            }

            UserActivity.UNAVAILABLE -> {
                binding.fab.setImageResource(R.drawable.ic_call_chat)
                binding.fab.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.inactive)
            }
        }
    }


    private fun onCallChatButtonClick() {
        if (!isFABOpen) {
            when (vm.userActivity.value) {
                UserActivity.CALL -> {
                    openCallDetail()
                }

                UserActivity.UNAVAILABLE -> {
                    vm.recheckActivities()
                }

                else -> {
                    vm.getUserCapabilities {
                        showFABMenu(it)
                    }
                }
            }
        } else {
            closeFABMenu()
        }
    }


    private fun showFABMenu(scope: List<String>?) {
        if (scope == null) return // Receive error. Processed in ViewModel

        if (scope.isEmpty()) { // CHAT and CALLS are not available. Setup server
            Toast.makeText(
                requireContext(),
                "CHAT and CALLS are not available. Setup server",
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        isFABOpen = true

        setupChatButton(scope.contains("chat"))
        setupCallButton(scope.contains("call"))

        binding.fabChat.animate().translationY(-resources.getDimension(R.dimen.standard_55))
        binding.fabCall.animate().translationY(-resources.getDimension(R.dimen.standard_105))
    }


    private fun setupChatButton(isEnable: Boolean) {
        if (isEnable) {
            binding.fabChat.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.accent)
            )
            binding.fabChat.isEnabled = true
        } else {
            binding.fabChat.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.inactive)
            )
            binding.fabChat.isEnabled = false
        }
    }


    private fun setupCallButton(isEnable: Boolean) {
        if (isEnable) {
            binding.fabCall.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.accent)
            )
            binding.fabCall.isEnabled = true
        } else {
            binding.fabCall.background.setTint(
                ContextCompat.getColor(requireContext(), R.color.inactive)
            )
            binding.fabCall.isEnabled = false
        }
    }


    private fun closeFABMenu() {
        isFABOpen = false
        binding.fabChat.animate().translationY(0F)
        binding.fabCall.animate().translationY(0F)
    }


    private fun openChatDetail() {
        findNavController()
            .navigate(R.id.action_FirstFragment_to_chatFragment)
    }


    private fun openCallDetail() {
        findNavController()
            .navigate(R.id.action_FirstFragment_to_callDetailFragment)
    }
}