package io.tl.mynhentai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.tl.mynhentai.ui.navigation.MainNavGraph
import io.tl.mynhentai.ui.theme.MyNHentaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyNHentaiTheme {
                MainNavGraph()
            }
        }
    }
}
