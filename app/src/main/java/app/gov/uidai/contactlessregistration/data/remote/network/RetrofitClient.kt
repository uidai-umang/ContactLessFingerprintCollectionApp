package app.gov.uidai.contactlessregistration.data.remote.network

import android.content.Context
import app.gov.uidai.contactlessregistration.BuildConfig
import app.gov.uidai.contactlessregistration.data.remote.api.ClfApiService
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TIMEOUT_SECONDS = 30L

    // Builds OkHttpClient with logging and Chucker for debug,
    // clean client for release
    fun buildOkHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // Logcat logging — prints full request/response body
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Chucker — visual HTTP inspector accessible via notification
            val chuckerCollector = ChuckerCollector(
                context = context,
                showNotification = true,
                retentionPeriod = RetentionManager.Period.ONE_HOUR
            )

            val chuckerInterceptor = ChuckerInterceptor.Builder(context)
                .collector(chuckerCollector)
                .maxContentLength(250_000L)
                .alwaysReadResponseBody(true)
                .build()

            builder
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor(chuckerInterceptor)
        }

        return builder.build()
    }

    // Builds Retrofit instance with Gson converter
    fun buildRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Creates ClfApiService from Retrofit instance
    fun buildApiService(retrofit: Retrofit): ClfApiService {
        return retrofit.create(ClfApiService::class.java)
    }
}