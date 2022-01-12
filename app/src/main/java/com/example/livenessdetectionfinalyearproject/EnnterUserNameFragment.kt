package com.example.livenessdetectionfinalyearproject

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.livenessdetectionfinalyearproject.base.launchActivity
import com.example.livenessdetectionfinalyearproject.base.toast
import com.example.livenessdetectionfinalyearproject.base.viewBinding
import com.example.livenessdetectionfinalyearproject.databinding.FragmentEnterUserNameBinding

class EnnterUserNameFragment : Fragment(R.layout.fragment_enter_user_name) {
    private val binding by viewBinding(FragmentEnterUserNameBinding::bind)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.apply {
            takePictureButton.setOnClickListener {
                if (usernameEditText.text.isBlank() || usernameEditText.text.length < 4) {
                    requireContext().toast("User name is too short")?.show()
                } else {
                    requireContext().launchActivity(activityClass = RegisterFaceActivity::class.java) {
                        putString(USER_NAME_PARAM, usernameEditText.text.toString().trim())
                    }
                }
            }
        }
    }
    companion object {
        const val USER_NAME_PARAM = "user_name_param"
    }
}