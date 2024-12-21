package com.example.healthcast.fragment

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.healthcast.AddActivity
import com.example.healthcast.DetailActivity
import com.example.healthcast.EditActivity
import com.example.healthcast.MainActivity
import com.example.healthcast.R
import com.example.healthcast.adapter.RecyclerAdapterStory
import com.example.healthcast.api.ApiRest
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.FragmentHome2Binding
import com.example.healthcast.model.CommentRequest
import com.example.healthcast.model.LikeRequest
import com.example.healthcast.model.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
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


class HomeFragment : Fragment() {
    lateinit var binding: FragmentHome2Binding
    lateinit var ctx: Context
    lateinit var dataList: ArrayList<Story>

    lateinit var recyclerAdapter: RecyclerAdapterStory

    private var currentImageUri: Uri? = null

    lateinit var launcherGallery: ActivityResultLauncher<PickVisualMediaRequest>

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Permission request granted", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_LONG)
                    .show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHome2Binding.inflate(layoutInflater)

        ctx = requireContext()

        dataList = ArrayList()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        getData()

        registerLaunchers()

        binding.btnAdd.setOnClickListener {
            startGallery()
        }

        return binding.root
    }

    fun initData() {
        recyclerAdapter = RecyclerAdapterStory(dataList!!, ctx)
        binding.rvData.adapter = recyclerAdapter
        recyclerAdapter!!.setOnclickCallback(object : RecyclerAdapterStory.OnItemClickCallback {
            override fun onItemClick(item: Story?, position: Int) {
                val intent = Intent(ctx, EditActivity::class.java)
                intent.putExtra("id", item!!.id)
                intent.putExtra("imageUrl", item!!.image_url)
                intent.putExtra("title", item.title)
                intent.putExtra("description", item.description)
                startActivityForResult(intent, 11)
            }

            override fun onItemDelete(item: Story?, position: Int) {
                val builder = AlertDialog.Builder(ctx)
                builder.setCancelable(true)
                builder.setTitle("Konfirmasi")
                builder.setMessage("Anda Yakin Ingin Menghapus Postingan Ini?")
                builder.setPositiveButton(
                    "Confirm"
                ) { dialog, which -> deleteItem(item!!.id.toString()) }
                builder.setNegativeButton(android.R.string.cancel) { dialog, which -> }

                val dialog = builder.create()
                dialog.show()
            }

            override fun onItemComment(item: Story?, position: Int) {
                val intent = Intent(ctx, DetailActivity::class.java)
                intent.putExtra("id", item!!.id)
                intent.putExtra("imageUrl", item!!.image_url)
                intent.putExtra("uiduser", item!!.user.id)
                intent.putExtra("title", item.title)
                intent.putExtra("description", item.description)
                intent.putStringArrayListExtra("likes", item.likes as ArrayList<String>)
                intent.putStringArrayListExtra("dislikes", item.dislikes as ArrayList<String>)
                startActivityForResult(intent, 11)
            }

            override fun onItemLike(item: Story?, position: Int) {
                like(item!!.id.toString(), position)
            }

            override fun onItemUnlike(item: Story?, position: Int) {
                dislike(item!!.id.toString())
            }
        })
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun registerLaunchers() {
        launcherGallery = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                val destinationUri =
                    Uri.fromFile(ctx.cacheDir.resolve("${System.currentTimeMillis()}.PNG"))
                uCropLauncher.launch(
                    UCrop.of(uri, destinationUri)
                        .withAspectRatio(1F, 1F)
                        .withMaxResultSize(2000, 2000)
                        .getIntent(requireContext())
                )
            } else {
                Log.d("Photo Picker", "No media selected")
            }
        }
    }

    private val uCropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                currentImageUri = resultUri
                showImage()
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("Crop Error", "UCrop error: $cropError")
            }
        }

    private fun showImage() {

        val intent = Intent(ctx, AddActivity::class.java)
        intent.putExtra("imageUri", currentImageUri.toString())
        startActivityForResult(intent, 10)

        currentImageUri?.let {
        }
    }

    fun getData() {
        val loading = ProgressDialog.show(ctx, null, "Harap Tunggu...", true, false)

        dataList.clear()

        val mApiClient: ApiRest = UtilsApi.getAPIService()
        mApiClient.getStory()
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(
                    call: Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    loading.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val json = JSONObject(response.body().toString())
                            val message = json.getString("message")

                            if (json.has("error")) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            } else {
                                val data = json.getJSONArray("data")
                                for (i in 0 until data.length()) {
                                    val mJsonString = data.getJSONObject(i).toString()
                                    val gson = Gson()
                                    try {
                                        // Parse each item in the JSONArray as a Perusahaan object
                                        val `object` = gson.fromJson(
                                            mJsonString,
                                            Story::class.java
                                        )
                                        dataList.add(`object`) // Add to the list
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                initData()
                            }

                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(ctx, "Kesalahan Parsing Data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(ctx, "Gagal Upload Data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.i("debug", "onFailure: ERROR > $t")
                    Toast.makeText(ctx, "Keneksi Error", Toast.LENGTH_SHORT).show()
                    loading.dismiss()
                }
            })
    }

    private fun deleteItem(id: String) {
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.deletePost(
            id
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
                                "Berhasil Dihapus",
                                Toast.LENGTH_SHORT
                            ).show()
                            getData()
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

    private fun like(id: String, position: Int) {
        val loading = ProgressDialog.show(ctx, null, "Harap Tunggu...", true, false)

        val requestBody = LikeRequest(FirebaseAuth.getInstance().currentUser!!.uid)

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.like(id, requestBody)
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
                                    "Like",
                                    Toast.LENGTH_SHORT
                                ).show()
                                getData()
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

    private fun dislike(id: String) {
        val loading = ProgressDialog.show(ctx, null, "Harap Tunggu...", true, false)

        val requestBody = LikeRequest(FirebaseAuth.getInstance().currentUser!!.uid)

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.dislike(id, requestBody)
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
                                    "Unlike",
                                    Toast.LENGTH_SHORT
                                ).show()
                                getData()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            getData()
        } else if (requestCode == 11 && resultCode == Activity.RESULT_OK) {
            getData()
        }
    }
}