package com.example.totowala

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.totowala.databinding.ActivityOtpVerificationBinding
import com.google.firebase.Timestamp
import kotlin.random.Random

class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpVerificationBinding
    private var generatedOtp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Verify OTP"

        // Generate and "send" OTP (in a real app, send via SMS)
        generatedOtp = generateOtp()
        Toast.makeText(this, "Your OTP is: $generatedOtp", Toast.LENGTH_LONG).show()

        binding.buttonVerifyOtp.setOnClickListener { verifyOtp() }
        binding.buttonBack.setOnClickListener { finish() }
    }

    private fun generateOtp(): String {
        val otp= Random.nextInt(100000, 999999).toString()
        binding.editTextOtp.setText(otp)
        return otp
    }

    private fun verifyOtp() {
        val enteredOtp = binding.editTextOtp.text.toString()

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
            return
        }

        if (enteredOtp == generatedOtp) {
            Toast.makeText(this, "otp matched", Toast.LENGTH_SHORT).show()
            val phoneNumber = intent.getStringExtra("phone") ?: return
            val name = intent.getStringExtra("name") ?: return
            val userType = intent.getStringExtra("type") ?: return
            val timestamp = Timestamp.now()

            // Create User object
            val user = User(
                name = name,
                phone = phoneNumber,
                type = userType,
                loggedIn = true,
                loggedInAt = timestamp.toDate().toString()
            )
            Toast.makeText(this, "saving User To LocalStorage", Toast.LENGTH_SHORT).show()
            UserManager.saveUserToLocalStorage(this,user);
            Toast.makeText(this, "saving User To Firebase", Toast.LENGTH_SHORT).show()
            UserManager.uploadUserToFirebase(user,
            onSuccess = {
                startActivity(Intent(this, AuctionsActivity::class.java))
                finish()
            }, onFailure = {
                Toast.makeText(this, "Error connecting to server. Try again!", Toast.LENGTH_SHORT).show()
            })
        }
    }
}
