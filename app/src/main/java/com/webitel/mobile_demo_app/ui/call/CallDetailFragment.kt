package com.webitel.mobile_demo_app.ui.call

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.databinding.FragmentCallDetailBinding
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.CallState


class CallDetailFragment : Fragment() {
    private val vm: CallsViewModel by activityViewModels()
    private var _binding: FragmentCallDetailBinding? = null
    private val binding get() = _binding!!

    private val scalesArg: Array<Float> = arrayOf(0.18F, 0.39F, 0.68F)
    private val alphasArg: Array<Float> = arrayOf(1F, 0.4F, 0.1F)
    private var draw = SonarDrawable(scalesArg, alphasArg, Color.BLUE)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCallDetailBinding.inflate(inflater, container, false)

        binding.sonarImageView.setImageDrawable(draw)
        binding.timerOnDetailImageView.text = ""
        binding.timerOnDetailImageView.format = "00:%s"

        binding.declineAnsweredCallImageButton.setOnClickListener {
            vm.disconnectCall()
        }

        binding.muteImageButton.setOnClickListener {
            vm.muteCall()
        }

        binding.loudspeakerImageButton.setOnClickListener {
            vm.toggleLoudspeaker()
        }

        binding.holdImageButton.setOnClickListener {
            vm.holdCall()
        }

        binding.numpadImageButton.setOnClickListener {

        }

        vm.callLive.observe(viewLifecycleOwner) { call ->
            call?.let {
                setNumpadButtonEnable(call.state)
                setMuteButtonEnable(call.state)
                setHoldButtonEnable(call.state)
                setLoudspeakerIcon(call.state.contains(CallState.LOUDSPEAKER))
                setCallInfo(call.state, call.answeredAt)
                if (call.state.contains(CallState.DISCONNECTED)) {
                    closeFragment()
                    vm.clearData()
                    Notifications.instance.cancelCallNotification()
                }
            }
        }

        vm.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                vm.clearData()
                closeFragment()
                Toast.makeText(
                    requireContext(),
                    "${err.code}\n${err.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.makeCall()
    }


    private fun setNumpadButtonEnable(state: List<CallState>) {
        binding.numpadImageButton.isEnabled = false
    }


    @SuppressLint("Range")
    private fun setMuteButtonEnable(state: List<CallState>) {
        val isEnable = (state.contains(CallState.ACTIVE))
        binding.muteImageButton.isEnabled = isEnable

        if (isEnable) {
            binding.muteImageButton.alpha = 1F
            setMuteIcon(state.contains(CallState.MUTE))
        } else {
            binding.muteImageButton.alpha = 0.3F
        }
    }


    private fun setMuteIcon(isOnMute: Boolean) {
        if (isOnMute) {
            val temp = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_mic_muted)
            binding.muteImageButton.setImageDrawable(temp)

        } else {
            val temp = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_mic)
            binding.muteImageButton.setImageDrawable(temp)
        }
    }


    private fun setHoldButtonEnable(state: List<CallState>) {
        val isEnable = (state.contains(CallState.ACTIVE))
        binding.holdImageButton.isEnabled = isEnable

        if (isEnable) {
            binding.holdImageButton.alpha = 1F
            setHoldIcon(state.contains(CallState.HOLD))
        } else {
            binding.holdImageButton.alpha = 0.3F
        }
    }


    private fun setHoldIcon(isOnHold: Boolean) {
        if (isOnHold) {
            DrawableCompat.setTint(
                binding.holdImageButton.drawable,
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.accent
                )
            )

        } else {
            DrawableCompat.setTint(
                binding.holdImageButton.drawable,
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.default_icon
                )
            )
        }
    }


    private fun setLoudspeakerIcon(isSpeaker: Boolean) {
        if (isSpeaker) {
            val temp = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_loudspeaker_on)
            binding.loudspeakerImageButton.setImageDrawable(temp)

        } else {
            val temp = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_loudspeaker_off)
            binding.loudspeakerImageButton.setImageDrawable(temp)
        }
    }


    private fun setCallInfo(state: List<CallState>, answeredAt: Long) {

        if (state.contains(CallState.ACTIVE)) {
            startTimer(answeredAt)
        } else {
            showCallState(state)
        }

        setSonar(state.contains(CallState.HOLD), answeredAt > 0)
    }


    @SuppressLint("SetTextI18n")
    private fun startTimer(answeredAt: Long) {
        binding.timerOnDetailImageView.text = "00:00:00"
        val duration = System.currentTimeMillis() - answeredAt
        binding.timerOnDetailImageView.base = (SystemClock.elapsedRealtime() - duration)

        binding.timerOnDetailImageView.setOnChronometerTickListener {
            val text: CharSequence = it.text
            if (text.length == 5) {
                it.text = "00:$text"
            } else if (text.length == 7) {
                it.text = "0$text"
            }
        }
        binding.timerOnDetailImageView.start()
        binding.callStateTextView.visibility = View.GONE
        binding.timerOnDetailImageView.visibility = View.VISIBLE
    }


    @SuppressLint("SetTextI18n")
    private fun showCallState(state: List<CallState>) {
        val callState = createCallState(state)
        binding.callStateTextView.text = callState
        binding.timerOnDetailImageView.visibility = View.GONE
        binding.callStateTextView.visibility = View.VISIBLE
        binding.timerOnDetailImageView.stop()
    }


    private fun createCallState(value: List<CallState>): String {
        return when {
            value.contains(CallState.REGISTERING_SIP_ACCOUNT) -> resources.getString(R.string.call_state_registering)
            value.contains(CallState.DIALING) -> resources.getString(R.string.call_state_dialing)
            value.contains(CallState.RINGING) -> resources.getString(R.string.call_state_ringing)
            value.contains(CallState.HOLD) -> resources.getString(R.string.call_state_hold)
            value.contains(CallState.MUTE) -> resources.getString(R.string.call_state_mute)
            value.contains(CallState.DISCONNECTED) -> resources.getString(R.string.call_state_disconnected)
            value.contains(CallState.ACTIVE) -> resources.getString(R.string.call_state_active)
            else -> "..."
        }
    }


    private fun setSonar(isOnHold: Boolean, isAnswered: Boolean) {
        val color = when {
            isOnHold -> {
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.accent
                )
            }

            isAnswered -> {
                Color.RED
            }

            else -> {
                Color.BLUE
            }
        }
        draw.setColor(color)
    }


    private fun closeFragment() {
        findNavController().popBackStack()
    }
}