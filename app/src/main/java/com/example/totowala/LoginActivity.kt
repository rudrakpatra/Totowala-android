package com.example.totowala

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.totowala.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //set defaults
        val storedUser = UserManager.getUserFromLocalStorage(this);
        if(storedUser != null){
            supportActionBar?.title = "Login As Existing User"
            binding.editTextPhone.setText(storedUser.phone)
            binding.editTextName.setText(storedUser.name)
            binding.radioPassenger.isChecked = storedUser.type == "passenger"
            binding.radioDriver.isChecked = storedUser.type == "driver"
        }
        else{
            supportActionBar?.title = "Login"
        }

        binding.buttonVerifyAndLogin.setOnClickListener {
            verifyAndLogin()
        }
    }

    private fun verifyAndLogin() {
        val name = binding.editTextName.text.toString()
        val phone = binding.editTextPhone.text.toString()
        val userType = if (binding.radioDriver.isChecked) "driver" else "passenger"

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, OtpVerificationActivity::class.java).apply {
            putExtra("phone", phone)
            putExtra("name", name)
            putExtra("type", userType)
        }
        startActivity(intent)
    }
}
