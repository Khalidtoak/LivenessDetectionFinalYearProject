package com.example.livenessdetectionfinalyearproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.livenessdetectionfinalyearproject.base.hide
import com.example.livenessdetectionfinalyearproject.base.launchActivity
import com.example.livenessdetectionfinalyearproject.base.show
import com.example.livenessdetectionfinalyearproject.databinding.ActivitySuccessBinding

class SuccessActivity : AppCompatActivity() {
    lateinit var binding : ActivitySuccessBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recognizeFace.setOnClickListener {
            binding.progressBar2.show()
            binding.linearLayout.hide()
            launchActivity(activityClass = RecognizeFaceCameraActivity::class.java)
            finish()
        }
    }
}