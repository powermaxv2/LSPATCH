package com.powermax.patchstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.powermax.patchstudio.ui.AppRoot
import com.powermax.patchstudio.ui.theme.PatchStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PatchStudioTheme {
                AppRoot()
            }
        }
    }
}
