package io.tl.mynhentai.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.tl.mynhentai.data.api.CdnRepository
import io.tl.mynhentai.data.api.MangaService
import io.tl.mynhentai.data.local.MangaDatabase
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.ui.detail.DetailViewModel
import io.tl.mynhentai.ui.home.HomeViewModel
import io.tl.mynhentai.ui.library.LibraryViewModel
import io.tl.mynhentai.ui.reader.ReaderViewModel
import io.tl.mynhentai.ui.search.SearchViewModel
import io.tl.mynhentai.ui.settings.SettingsViewModel
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .dispatcher(Dispatcher().apply {
                maxRequestsPerHost = 20
                maxRequests = 100
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "MyNHentai/1.0 (https://github.com/your-repo)")
                        .build()
                )
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    single {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        retrofit2.Retrofit.Builder()
            .baseUrl("https://nhentai.net/")
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single { get<retrofit2.Retrofit>().create(MangaService::class.java) }
    single { CdnRepository(get()) }
}

val imageModule = module {
    single<ImageLoader> {
        val ctx: Context = androidContext()
        val client: OkHttpClient = get()
        val memCache = MemoryCache.Builder(ctx)
            .maxSizePercent(0.25)
            .build()
        val diskCache = DiskCache.Builder()
            .directory(ctx.cacheDir.resolve("coil_cache"))
            .maxSizeBytes(250L * 1024 * 1024)
            .build()
        ImageLoader.Builder(ctx)
            .okHttpClient(client)
            .memoryCache(memCache)
            .diskCache(diskCache)
            .build()
    }
}

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MangaDatabase::class.java,
            "mynhentai.db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<MangaDatabase>().mangaDao() }
}

val repositoryModule = module {
    single { MangaRepository(get(), get(), get()) }
    single { SettingsHelper(androidContext()) }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { DetailViewModel(get(), get()) }
    viewModel { ReaderViewModel(get(), get()) }
    viewModel { LibraryViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
