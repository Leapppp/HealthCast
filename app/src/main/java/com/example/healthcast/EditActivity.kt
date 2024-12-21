package com.example.healthcast

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.ActivityEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.yalantis.ucrop.UCrop
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

class EditActivity : AppCompatActivity() {
    lateinit var binding: ActivityEditBinding
    lateinit var context: Context

    private var currentImageUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(this@EditActivity, "Permission request denied", Toast.LENGTH_LONG)
                    .show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this@EditActivity,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var id: String? = null
    var name: String? = null
    var description: String? = null
    var photoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this@EditActivity

        id = intent!!.getStringExtra("id")
        photoUrl = intent!!.getStringExtra("imageUrl")
        title = intent!!.getStringExtra("title")
        description = intent!!.getStringExtra("description")

        Glide.with(context).load(photoUrl).into(binding.imgvImage)

        binding.etTitle.setText(title)
        binding.etDeskription.setText(description)

        binding.btnAdd.setOnClickListener {
            saveData()
        }

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.ubahPhoto.setOnClickListener {
            startGallery()
        }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            UCrop.of(uri, Uri.fromFile(cacheDir.resolve("${System.currentTimeMillis()}.jpg")))
                .withAspectRatio(1F, 1F)
                .withMaxResultSize(2000, 2000)
                .start(this)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            currentImageUri = resultUri
            showImage()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Log.e("Crop Error", "onActivityResult: $cropError")
        }
    }


    private fun showImage() {
        Glide.with(this).load(currentImageUri).into(binding.imgvImage)
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
        if (currentImageUri != null) {
            val filePath = getRealPathFromURIPath(currentImageUri!!, this@EditActivity)
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
        mApiRest.editPost(
            id, uuid, title, deskripsi, image
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
                                "Berhasil Mengubah Postingan",
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