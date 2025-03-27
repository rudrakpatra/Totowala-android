package com.example.totowala

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Define the User data class
data class User(
    val name: String ="",
    val phone: String="",
    val type: String="",
    var loggedIn: Boolean=true,
    var loggedInAt: String="",
)
object UserManager {
    private const val PREFS_NAME = "TotowalaPrefs"
    private const val USER_KEY = "user"
    private val gson = Gson()

    // Save user to local storage
    fun saveUserToLocalStorage(context: Context, user: User) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = gson.toJson(user)
        with(sharedPref.edit()) {
            putString(USER_KEY, userJson)
            apply()
        }
    }

    // Load user from local storage
    fun getUserFromLocalStorage(context: Context): User? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = sharedPref.getString(USER_KEY, null) ?: return null
        return gson.fromJson(userJson, object : TypeToken<User>() {}.type)
    }

    // Remove user from local storage
    private fun removeUserFromLocalStorage(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(USER_KEY)
            apply()
        }
    }

    // Remove user from Firebase
    private fun removeUserFromFirebase(phone: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(phone)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    private fun logOutUserFromFirebase(phone: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(phone)
            .update("loggedIn", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun logOutUser(context: Context, user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        logOutUserFromFirebase(user.phone,   // Remove from Firebase
            onSuccess = {
                Log.d("UserManager", "User Logged Out successfully")
                onSuccess()
            },
            onFailure = { exception ->
                Log.e("UserManager", "Failed to remove user from Firebase: ${exception.message}")
                onFailure(exception)
            }
        )
    }

    // Upload user to Firebase
    fun uploadUserToFirebase(user: User, onSuccess: (Any?) -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.phone)
            .set(user)
            .addOnSuccessListener { onSuccess(null) }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Fetch user from Firebase using phone number
    private fun fetchUserFromFirebase(phone: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(phone)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    onSuccess(user)
                } else {
                    onSuccess(null) // User does not exist
                }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    fun checkExistingUser(context: Context, phone: String, onResult: (User?) -> Unit) {
        val localUser = getUserFromLocalStorage(context)
        if(localUser != null && localUser.phone == phone) {
            fetchUserFromFirebase(localUser.phone,
                onSuccess = { user ->
                    if (user != null) saveUserToLocalStorage(
                        context,
                        user
                    ) // Save user locally if found
                    Log.d("UserManager","saving user")
                    onResult(user)
                },
                onFailure = { exception ->
                    Log.e("UserManager", "Error fetching user: ${exception.message}")
                    onResult(null)
                })
        }
        else{
            onResult(null)
        }
    }
}