package com.example.healthcast.fragment

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.healthcast.R
import com.example.healthcast.api.ApiRest
import com.example.healthcast.api.UtilsApi
import com.example.healthcast.databinding.FragmentProfileBinding
import com.example.healthcast.model.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding

    lateinit var ctx: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        ctx = requireContext()

        getData()

        return binding.root
    }

    fun getData() {
        val loading = ProgressDialog.show(ctx, null, "Harap Tunggu...", true, false)
        val mApiClient: ApiRest = UtilsApi.getAPIService()
        mApiClient.getUser(FirebaseAuth.getInstance().currentUser!!.uid)
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
                                val data = json.getJSONObject("data")
                                binding.etName.setText(data.getString("full_name"))
                                binding.etEmail.setText(data.getString("email"))
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
}