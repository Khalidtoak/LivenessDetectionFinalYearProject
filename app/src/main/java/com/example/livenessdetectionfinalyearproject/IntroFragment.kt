
package com.example.livenessdetectionfinalyearproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.livenessdetectionfinalyearproject.base.hide
import com.example.livenessdetectionfinalyearproject.base.launchActivity
import com.example.livenessdetectionfinalyearproject.base.show
import com.example.livenessdetectionfinalyearproject.base.viewBinding
import com.example.livenessdetectionfinalyearproject.databinding.FragmentIntroBinding

class IntroFragment : Fragment(R.layout.fragment_intro) {

    private val binding by viewBinding(FragmentIntroBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            loginButton.setOnClickListener {
                progressBar.show()
                loginButton.hide()
                registerFaceButton.hide()
                requireContext().launchActivity(activityClass = RecognizeFaceCameraActivity::class.java)
            }

            registerFaceButton.setOnClickListener {
                findNavController().navigate(IntroFragmentDirections.actionIntroFragmentToSignUpNameFragment())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.apply {
            progressBar.hide()
            loginButton.show()
            registerFaceButton.show()
        }
    }
}