package com.example.totowala

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Create a layout with a progress bar

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE  // Show progress bar

        val storedUser = UserManager.getUserFromLocalStorage(this)

        if (storedUser != null) {
            UserManager.checkExistingUser(this, storedUser.phone) { existingUser ->
                progressBar.visibility = View.GONE // Hide progress bar after verification

                val nextActivity = if (existingUser == null || !existingUser.loggedIn) {
                    LoginActivity::class.java
                } else {
                    AuctionsActivity::class.java
                }

                val intent = Intent(this, nextActivity)
                startActivity(intent)
                finish()
            }
        } else {
            progressBar.visibility = View.GONE
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
