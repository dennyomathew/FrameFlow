package com.dennymathew.frameflow.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.remote.RateLimitRetryInterceptor
import com.dennymathew.frameflow.data.remote.RickAndMortyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ImageDatabase {
        return Room.databaseBuilder(
            context,
            ImageDatabase::class.java,
            ImageDatabase.DATABASE_NAME
        ).addMigrations(*ImageDatabase.ALL_MIGRATIONS).build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(RateLimitRetryInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRickAndMortyApi(okHttpClient: OkHttpClient): RickAndMortyApi {
        return Retrofit.Builder()
            .baseUrl(RickAndMortyApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RickAndMortyApi::class.java)
    }

    /**
     * Coil's image loader. Shares the API client's connection pool but not its body logging,
     * caps parallel downloads so a fast scroll doesn't trip the server's rate limit, and retries
     * the requests that still get HTTP 429.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        val imageClient = okHttpClient.newBuilder()
            .apply { interceptors().clear() }
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = MAX_IMAGE_REQUESTS_PER_HOST })
            .addInterceptor(RateLimitRetryInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(imageClient)
            .crossfade(true)
            .build()
    }

    private const val MAX_IMAGE_REQUESTS_PER_HOST = 4
}
