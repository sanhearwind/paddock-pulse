package com.f1pulse.app.core.network

import com.f1pulse.app.data.remote.jolpica.JolpicaApi
import com.f1pulse.app.data.remote.openf1.OpenF1Api
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class JolpicaClient
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class OpenF1Client
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class JolpicaRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class OpenF1Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val JOLPICA_BASE = "https://api.jolpi.ca/ergast/f1/"
    private const val OPENF1_BASE = "https://api.openf1.org/v1/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    /**
     * One OkHttp client with per-host token buckets.
     *
     * OpenF1's free tier caps at 3 requests/second **and** 30 requests/minute; the
     * per-minute ceiling is the binding one once anything polls, so both are enforced.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient {
        val rateInterceptor = RateLimitInterceptor(
            mapOf(
                "jolpi.ca" to TokenBucket(ratePerSecond = 4.0),
                "openf1.org" to TokenBucket(ratePerSecond = 3.0, perMinute = 30),
            ),
        )

        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(rateInterceptor)
            .addInterceptor(logging)

        // Restrict to modern TLS. Cleartext is deliberately not allowed: both APIs are HTTPS.
        builder.connectionSpecs(
            listOf(
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build(),
            )
        )
        return builder.build()
    }

    @Provides
    @Singleton
    @JolpicaRetrofit
    fun provideJolpicaRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(JOLPICA_BASE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    @OpenF1Retrofit
    fun provideOpenF1Retrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(OPENF1_BASE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideJolpicaApi(@JolpicaRetrofit retrofit: Retrofit): JolpicaApi =
        retrofit.create(JolpicaApi::class.java)

    @Provides
    @Singleton
    fun provideOpenF1Api(@OpenF1Retrofit retrofit: Retrofit): OpenF1Api =
        retrofit.create(OpenF1Api::class.java)
}
