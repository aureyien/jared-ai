package com.music.sttnotes

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.rememberNavController
import com.music.sttnotes.ui.components.VolumeScrollHandler
import com.music.sttnotes.ui.navigation.NavGraph
import com.music.sttnotes.ui.theme.EInkWhite
import com.music.sttnotes.ui.theme.WhisperNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Track volume scroll handler
    // The handler will only be set when volume scrolling is enabled AND a screen registers one
    // @Volatile ensures visibility across threads (compose coroutines + main thread)
    @Volatile
    private var volumeScrollHandler: VolumeScrollHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhisperNotesTheme {
                val focusManager = LocalFocusManager.current
                // Full screen white background, content respects system bars
                // Tap outside input fields dismisses the keyboard
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EInkWhite)
                        .systemBarsPadding()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        }
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept volume buttons for scrolling when a handler is registered
        if (volumeScrollHandler != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    volumeScrollHandler?.scrollUp()
                    return true // Consume event
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeScrollHandler?.scrollDown()
                    return true // Consume event
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Update the volume scroll handler from composables
     * Screens should call this when they mount/unmount
     * Only set when volume scrolling is enabled in settings
     */
    fun setVolumeScrollHandler(handler: VolumeScrollHandler?) {
        volumeScrollHandler = handler
    }
}
