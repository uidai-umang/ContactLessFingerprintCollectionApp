package app.gov.uidai.contactlessregistration.data.remote.network

import android.content.Context
import app.gov.uidai.contactlessregistration.BuildConfig
import app.gov.uidai.contactlessregistration.data.remote.api.ClfApiService
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TIMEOUT_SECONDS = 30L

    // Provides a single reusable Gson instance across the app.
    // Centralizing this makes it easy to add date formats or
    // naming policies later without touching call sites.
    private fun provideGson(): Gson =
        GsonBuilder()
            .setLenient()
            .create()

    // Logcat logging — prints full request/response body, debug only
    private fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    // Chucker — visual HTTP inspector accessible via notification, debug only
    private fun provideChuckerInterceptor(context: Context): Interceptor {
        val collector = ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
        return ChuckerInterceptor.Builder(context)
            .collector(collector)
            .maxContentLength(250_000L)
            .alwaysReadResponseBody(true)
            .build()
    }

    // Builds OkHttpClient with debug-only interceptors (logging, Chucker).
    // Release builds get a clean client with no logging overhead.
    fun buildOkHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder
                .addInterceptor(provideLoggingInterceptor())
                .addNetworkInterceptor(provideChuckerInterceptor(context))
        }

        return builder.build()
    }

    // Builds Retrofit instance pointed at our backend, using shared Gson config
    fun buildRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(provideGson()))
            .build()

    // Creates the typed API service from the Retrofit instance
    fun buildApiService(retrofit: Retrofit): ClfApiService =
        retrofit.create(ClfApiService::class.java)
}