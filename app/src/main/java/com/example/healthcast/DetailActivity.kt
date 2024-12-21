package com.example.healthcast

import android.app.ProgressDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.healthcast.adapter.RecyclerAdapterComment
import com.example.healthcast.api.ApiRest
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.ActivityDetailBinding
import com.example.healthcast.model.Comment
import com.example.healthcast.model.CommentRequest
import com.example.healthcast.model.LikeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    lateinit var context: Context
    lateinit var dataList: ArrayList<Comment>
    var id: String? = null
    var name: String? = null
    var description: String? = null
    var photoUrl: String? = null
    var uiduser: String? = null
    var likes : ArrayList<String>? = null
    var dislikes : ArrayList<String>? = null
    var liked = false
    var disliked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this@DetailActivity

        id = intent!!.getStringExtra("id")
        photoUrl = intent!!.getStringExtra("imageUrl")
        title = intent!!.getStringExtra("title")
        description = intent!!.getStringExtra("description")
        uiduser = intent!!.getStringExtra("uiduser")
        likes = intent!!.getStringArrayListExtra("likes")
        dislikes = intent!!.getStringArrayListExtra("dislikes")

        Glide.with(context).load(photoUrl).into(binding.imgvImage)

        dataList = ArrayList()

        binding.tvNama.text = title
        binding.tvDekripsi.text = description

        binding.btnSend.setOnClickListener {
            comment()
        }

        val uuid = FirebaseAuth.getInstance().currentUser!!.uid

        if (likes!!.contains(uuid)) {
            liked = true
        }

        if (dislikes!!.contains(uuid)) {
            disliked = true
        }

        setHightlightButton()

        if (uuid == uiduser) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.btnEdit.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnLike.setOnClickListener {
            like(id!!, 0)
        }
        binding.btnUnlike.setOnClickListener {
            dislike(id!!)
        }

        getData()

    }

    fun setHightlightButton(){
        if(liked){
            binding.btnLike.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_primary));
        }else {
            binding.btnLike.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.black));
        }

        if(disliked){
            binding.btnUnlike.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.color_primary));
        } else {
            binding.btnUnlike.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.black));
        }
    }

    fun initData() {
        val recyclerAdapter = RecyclerAdapterComment(dataList, context)
        binding.rvData.adapter = recyclerAdapter
        recyclerAdapter.setOnclickCallback(object : RecyclerAdapterComment.OnItemClickCallback {
            override fun onItemClick(item: Comment?, position: Int) {
//                val intent = Intent(context, EditActivity::class.java)
//                intent.putExtra("id", item!!.id)
//                intent.putExtra("imageUrl", item!!.image_url)
//                intent.putExtra("title", item.title)
//                intent.putExtra("description", item.description)
//                startActivityForResult(intent, 11)
            }

            override fun onItemDelete(item: Comment?, position: Int) {
//                val builder = AlertDialog.Builder(context)
//                builder.setCancelable(true)
//                builder.setTitle("Konfirmasi")
//                builder.setMessage("Anda Yakin Ingin Menghapus Proyek Ini?")
//                builder.setPositiveButton(
//                    "Confirm"
//                ) { dialog, which -> deleteItem(item!!.id.toString()) }
//                builder.setNegativeButton(android.R.string.cancel) { dialog, which -> }
//
//                val dialog = builder.create()
//                dialog.show()
            }
        })
    }

    fun getData() {
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)

        dataList.clear()

        val mApiClient: ApiRest = UtilsApi.getAPIService()
        mApiClient.getComment(id)
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
                                            Comment::class.java
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
                            Toast.makeText(context, "Kesalahan Parsing Data", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(context, "Gagal Upload Data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.i("debug", "onFailure: ERROR > $t")
                    Toast.makeText(context, "Keneksi Error", Toast.LENGTH_SHORT).show()
                    loading.dismiss()
                }
            })
    }

    private fun comment() {
        val loading = ProgressDialog.show(this@DetailActivity, null, "Harap Tunggu...", true, false)
        val comment = binding.etComment.text.toString().trim()
        val requestBody = CommentRequest(FirebaseAuth.getInstance().currentUser!!.uid, comment)

        val mApiRest = UtilsApi.getAPIService()
        mApiRest.comment(id, requestBody)
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
                                    "Berhasil Comment",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.etComment.setText("")
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

    private fun like(id: String, position: Int) {
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)

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
                                liked = !liked

                                if(liked){
                                    disliked = false
                                }

                                setHightlightButton()

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
        val loading = ProgressDialog.show(context, null, "Harap Tunggu...", true, false)

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
                                disliked = !disliked

                                if(disliked){
                                    liked = false
                                }

                                setHightlightButton()

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
}