package com.example.healthcast

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.ActivityAddBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class AddActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddBinding
    lateinit var context: Context
    var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this@AddActivity

        imageUri = Uri.parse(intent!!.getStringExtra("imageUri"))

        binding.imgvImage.setImageURI(imageUri)

        binding.btnAdd.setOnClickListener {
            saveData()
        }
    }

    private fun getRealPathFromURIPath(contentURI: Uri, activity: Activity): String? {
        val cursor = activity.contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) {
            return contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            return cursor.getString(idx)
        }
    }

    private fun saveData() {
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)

        var image: MultipartBody.Part? = null
        if (imageUri != null) {
            val filePath = getRealPathFromURIPath(imageUri!!, this@AddActivity)
            val file = File(filePath)

            // Determine the MIME type based on the file
            val mimeType = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                else -> null
            }

            if (mimeType != null) {
                val mFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                image = MultipartBody.Part.createFormData("image", file.name, mFile)
            } else {
                Log.e("Upload Error", "Unsupported file type")
                return
            }
        }

        val uuid = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        val title = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            binding.etTitle.text.toString().ifBlank { "" })
        val deskripsi = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            binding.etDeskription.text.toString().ifBlank { "" })

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.savePost(
            uuid, title, deskripsi, image
        ).enqueue(object : Callback<JsonObject?> {
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
                                "Berhasil Membuat Postingan",
                                Toast.LENGTH_SHORT
                            ).show()
                            setResult(RESULT_OK)
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
                Log.d("messageerror", t.message!!)
                Toast.makeText(context, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}