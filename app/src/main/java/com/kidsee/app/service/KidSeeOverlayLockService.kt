package com.kidsee.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kidsee.core.ui.components.KidSeeMascot
import com.kidsee.core.ui.theme.KidSeeTypography
import com.kidsee.core.ui.theme.WazeBlue
import com.kidsee.core.ui.theme.WazeWarmWhite
import timber.log.Timber

/**
 * Fullscreen lock overlay that displays over all apps when screen is locked by parent or schedule.
 */
class KidSeeOverlayLockService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        var isShowing = false
            private set

        fun show(context: Context) {
            val intent = Intent(context, KidSeeOverlayLockService::class.java)
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, KidSeeOverlayLockService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
        isShowing = true
    }

    private fun showOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KidSeeOverlayLockService)
            setViewTreeSavedStateRegistryOwner(this@KidSeeOverlayLockService)
            setContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WazeWarmWhite)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    KidSeeMascot(
                        size = 130.dp,
                        isHappy = false,
                        pupilOffsetY = 0.5f
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Time for a Break!",
                        style = KidSeeTypography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Screen locked by your parent.\nTalk with Mom or Dad to unlock.",
                        style = KidSeeTypography.bodyLarge.copy(
                            fontSize = 17.sp,
                            color = Color(0xFF67727E)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        overlayView = composeView
        try {
            windowManager?.addView(overlayView, params)
            Timber.d("Lock overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "Error showing lock overlay")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Error removing lock overlay")
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
