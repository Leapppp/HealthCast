package com.example.healthcast

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.ActivitySignUpBinding
import com.example.healthcast.model.RegisterRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this@SignUpActivity

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.buttonSignup.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    val loading = ProgressDialog.show(
                        this@SignUpActivity,
                        null,
                        "Harap Tunggu...",
                        true,
                        false
                    )
                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        loading.dismiss()
                        if (it.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Sign up successful, please login",
                                Toast.LENGTH_SHORT
                            ).show()
                            register()
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun register() {
        val loading = ProgressDialog.show(this@SignUpActivity, null, "Harap Tunggu...", true, false)

        val emailet = binding.emailEt.text.toString().trim()
        val nameet = binding.nameEt.text.toString().trim();

        val requestBody = RegisterRequest(firebaseAuth.currentUser!!.uid, emailet, nameet)

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.register(requestBody)
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    loading.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val json = JSONObject(response.body().toString())

                            val message = json.getString("message")

                            if (json.has("error")) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Berhasil Register",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Gagal Upload Data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    loading.dismiss()
                    Log.d("messageerror", t.message ?: "Unknown error")
                    Toast.makeText(context, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            })
    }


}