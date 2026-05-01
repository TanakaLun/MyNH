package io.tl.mynhentai

import android.app.Application
import io.tl.mynhentai.di.databaseModule
import io.tl.mynhentai.di.imageModule
import io.tl.mynhentai.di.networkModule
import io.tl.mynhentai.di.repositoryModule
import io.tl.mynhentai.di.viewModelModule
import io.tl.mynhentai.ui.components.NotificationHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyNHentaiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        startKoin {
            androidContext(this@MyNHentaiApp)
            modules(
                networkModule,
                imageModule,
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
