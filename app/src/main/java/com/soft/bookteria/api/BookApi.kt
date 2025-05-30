package com.soft.bookteria.api

import android.content.Context;
import com.soft.bookteria.api.models.BookCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URLEncoder

class BookApi(context: Context) {
    private val baseUrl = "https://gutenberg-backend-o7ffixyxs-steve859s-projects.vercel.app/books"
    
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    
    private suspend fun makeApiRequest(request: Request): Result<BookCollection> =
        suspendCoroutine { continuation ->
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(Result.failure(exception = e))
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    continuation.resume(
                        Result.success(
                            json.decodeFromString(
                                BookCollection.serializer(),
                                response.body!!.string()
                            ).copy(isCached = response.cacheResponse != null)
                        )
                    )
                }
            }
        })
    }
    
    suspend fun searchBooks(query: String): Result<BookCollection>{
        val encodedString = withContext(Dispatchers.IO){
            URLEncoder.encode(query, "UTF-8")
        }
        val request = Request.Builder().get().url("${baseUrl}?search=$encodedString").build()
        return makeApiRequest(request)
    }
    
    suspend fun getBookById(bookId: String): Result<BookCollection>{
        val request = Request.Builder().get().url("${baseUrl}?ids=$bookId").build()
        return makeApiRequest(request)
    }
    
    suspend fun getAllBooks(
        page: Long,
    ): Result<BookCollection> {
        val url = "${baseUrl}?page=$page"
        val request = Request.Builder().get().url(url).build()
        return makeApiRequest(request)
    }
    
    suspend fun getBookByCategory(
        category: String,
        page: Long
    ): Result<BookCollection>{
        val url = "${baseUrl}?page=$page&topic=$category"
        val request = Request.Builder().get().url(url).build()
        return makeApiRequest(request)
    }
}