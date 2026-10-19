package com.zscreencontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zscreencontrol.ui.MainScreen
import com.zscreencontrol.ui.theme.ZScreenTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZScreenTheme {
                MainScreen()
            }
        }
    }
}
