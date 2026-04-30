package io.tl.mynhentai

import android.app.Application
import io.tl.mynhentai.di.databaseModule
import io.tl.mynhentai.di.networkModule
import io.tl.mynhentai.di.repositoryModule
import io.tl.mynhentai.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyNHentaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyNHentaiApp)
            modules(
                networkModule,
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
