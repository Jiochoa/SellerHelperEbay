package com.example.sellerhelperebay.data

import android.content.Context
import com.example.sellerhelperebay.BuildConfig
import com.example.sellerhelperebay.data.ai.GeminiAnalyzer
import com.example.sellerhelperebay.data.db.AppDatabase
import com.example.sellerhelperebay.data.ebay.EbayAuthApi
import com.example.sellerhelperebay.data.ebay.EbayAuthManager
import com.example.sellerhelperebay.data.ebay.EbayConfig
import com.example.sellerhelperebay.data.ebay.EbayListingApi
import com.example.sellerhelperebay.data.ebay.EbayRepository
import com.example.sellerhelperebay.data.ebay.EbayTokenStore
import com.example.sellerhelperebay.data.images.ImageStore
import com.example.sellerhelperebay.data.repo.ItemRepository
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class AppContainer(appContext: Context) {
    private val database: AppDatabase = AppDatabase.build(appContext)
    val imageStore: ImageStore = ImageStore(appContext)
    private val analyzer: GeminiAnalyzer = GeminiAnalyzer()
    val itemRepository: ItemRepository = ItemRepository(database, imageStore, analyzer)

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(EbayConfig.apiBaseUrl + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val ebayAuthManager: EbayAuthManager = EbayAuthManager(
        authApi = retrofit.create(EbayAuthApi::class.java),
        tokenStore = EbayTokenStore(appContext)
    )

    val ebayRepository: EbayRepository = EbayRepository(
        db = database,
        listingApi = retrofit.create(EbayListingApi::class.java),
        authManager = ebayAuthManager
    )
}
