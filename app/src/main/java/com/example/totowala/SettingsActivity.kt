package com.example.totowala

import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //heading to settings
        supportActionBar?.title = "Settings"

        // Retrieve user data from local storage
        val user = UserManager.getUserFromLocalStorage(this)

        // Update UI with user details if available
        findViewById<TextView>(R.id.txtUserName).setText("User: ${user?.name ?: "Unknown"}")
        findViewById<TextView>(R.id.txtUserPhone).setText("Phone: ${user?.phone ?: "N/A"}")
        findViewById<TextView>(R.id.txtUserType).setText("Type: ${user?.type ?: "N/A"}")
        // Remove user button
        findViewById<Button>(R.id.btnRemoveUser).setOnClickListener {
            user?.let {
                val progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Logging Out...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                UserManager.logOutUser(
                    this,
                    it,
                    onSuccess = {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Logged Out successfully", Toast.LENGTH_SHORT).show()
                        redirectToLogin()
                    },
                    onFailure = { exception ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to log out: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
