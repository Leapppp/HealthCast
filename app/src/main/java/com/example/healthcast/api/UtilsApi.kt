package com.example.healthcast.api

object UtilsApi {
    const val BASE_URL_API: String = "https://healthcast-api-254230550415.asia-southeast2.run.app/"

    fun getAPIService(): ApiRest {
        val apiClient: ApiClient = ApiClient
        return apiClient.getClient(BASE_URL_API)!!.create(ApiRest::class.java)
    }

    const val BASE_URL_API_WHEATHER: String = "https://api.openweathermap.org/data/2.5/"
    fun getAPIServiceWheather(): ApiRest {
        val apiClient: ApiClient = ApiClient
        return apiClient.getClient2(BASE_URL_API_WHEATHER)!!.create(ApiRest::class.java)
    }
}

