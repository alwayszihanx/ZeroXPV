package io.alwayszihan.zeroxpv.ui.iptv.component

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import io.alwayszihan.zeroxpv.ui.iptv.player.Media3Player

private class IptvPlayerTextureView @JvmOverloads constructor(
    context: Context,
    private val player: Media3Player? = null,
    initialAspectRatio: Int = 0,
    initialMirrorFlip: Boolean = false,
    private val onSurfaceReady: () -> Unit = {}
) : TextureView(context) {

    private var videoWidth = 0
    private var videoHeight = 0
    private var videoSarNum = 0
    private var videoSarDen = 0
    private var videoRotationDegree = 0
    private var currentAspectRatio = initialAspectRatio
    private var currentMirrorFlip = initialMirrorFlip
    
    init {
        isOpaque = true
        layoutParams = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        )
        
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val surf = android.view.Surface(surfaceTexture)
                player?.attachSurface(surf)
                post { 
                    applyTransform()
                    requestLayout()
                }
                onSurfaceReady()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                post { applyTransform() }
            }

            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                player?.attachSurface(null)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            applyTransform()
        }
    }
    
    private fun applyTransform() {
        if (width > 0) {
            val matrix = Matrix()
            if (currentMirrorFlip) {
                matrix.setScale(-1f, 1f, width / 2f, 0f)
            } else {
                matrix.setScale(1f, 1f, width / 2f, 0f)
            }
            setTransform(matrix)
        }
    }
    
    fun updateMirrorFlip(flip: Boolean) {
        if (currentMirrorFlip != flip) {
            currentMirrorFlip = flip
            applyTransform()
        }
    }
    
    fun updateAspectRatio(ratio: Int) {
        if (currentAspectRatio != ratio) {
            currentAspectRatio = ratio
            requestLayout()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val vWidth = player?.exoPlayer?.videoSize?.width ?: 0
        val vHeight = player?.exoPlayer?.videoSize?.height ?: 0
        
        if (vWidth == 0 || vHeight == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            videoWidth = vWidth
            videoHeight = vHeight
            doMeasure(widthMeasureSpec, heightMeasureSpec, currentAspectRatio)
            setMeasuredDimension(measuredWidth, measuredHeight)
        }
    }
    
    private var measuredWidth = 0
    private var measuredHeight = 0
    
    private fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int, aspectRatio: Int) {
        currentAspectRatio = aspectRatio
        
        if (videoHeight == 0 || videoWidth == 0) {
            measuredWidth = 1
            measuredHeight = 1
            return
        }
        
        var wSpec = widthMeasureSpec
        var hSpec = heightMeasureSpec
        
        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
            val temp = wSpec
            wSpec = hSpec
            hSpec = temp
        }
        
        var realWidth = videoWidth
        if (videoSarNum != 0 && videoSarDen != 0) {
            val pixelWidthHeightRatio = videoSarNum / (videoSarDen / 1.0)
            realWidth = (pixelWidthHeightRatio * videoWidth).toInt()
        }
        
        var width = View.getDefaultSize(realWidth, wSpec)
        var height = View.getDefaultSize(videoHeight, hSpec)
        
        if (realWidth > 0 && videoHeight > 0) {
            val widthSpecMode = View.MeasureSpec.getMode(wSpec)
            val widthSpecSize = View.MeasureSpec.getSize(wSpec)
            val heightSpecMode = View.MeasureSpec.getMode(hSpec)
            val heightSpecSize = View.MeasureSpec.getSize(hSpec)
            
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
                val displayAspectRatio = when (currentAspectRatio) {
                    0 -> {
                        var ratio = 16.0f / 9.0f
                        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                            ratio = 1.0f / ratio
                        }
                        ratio
                    }
                    1 -> {
                        var ratio = 4.0f / 3.0f
                        if (videoRotationDegree == 90 || videoRotationDegree == 270) {
                            ratio = 1.0f / ratio
                        }
                        ratio
                    }
                    2, 3 -> {
                        var ratio = realWidth.toFloat() / videoHeight.toFloat()
                        if (videoSarNum > 0 && videoSarDen > 0) {
                            ratio = ratio * videoSarNum / videoSarDen
                        }
                        ratio
                    }
                    else -> realWidth.toFloat() / videoHeight.toFloat()
                }
                
                val shouldBeWider = displayAspectRatio > specAspectRatio
                
                when (currentAspectRatio) {
                    0, 1, 3 -> {
                        if (shouldBeWider) {
                            width = widthSpecSize
                            height = (width / displayAspectRatio).toInt()
                        } else {
                            height = heightSpecSize
                            width = (height * displayAspectRatio).toInt()
                        }
                    }
                    2 -> {
                        width = widthSpecSize
                        height = heightSpecSize
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = heightSpecSize
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                width = widthSpecSize
                height = width * videoHeight / realWidth
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                height = heightSpecSize
                width = height * realWidth / videoHeight
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize
                }
            } else {
                width = realWidth
                height = videoHeight
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    height = heightSpecSize
                    width = height * realWidth / videoHeight
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    width = widthSpecSize
                    height = width * videoHeight / realWidth
                }
            }
        }
        
        measuredWidth = width
        measuredHeight = height
    }
}

@Composable
fun IptvPlayerSurface(
    player: Media3Player?,
    aspectRatio: Int,
    modifier: Modifier = Modifier,
    mirrorFlip: Boolean = false,
    onSurfaceReady: () -> Unit = {},
    currentChannelUrl: String = ""
) {
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }
    var surfaceReady by remember { mutableStateOf(true) }
    var lastChannelUrl by remember { mutableStateOf(currentChannelUrl) }
    
    LaunchedEffect(currentChannelUrl) {
        if (currentChannelUrl.isNotEmpty() && currentChannelUrl != lastChannelUrl) {
            lastChannelUrl = currentChannelUrl
            surfaceReady = false
            player?.attachSurface(null)
            kotlinx.coroutines.delay(50)
            surfaceReady = true
        } else if (lastChannelUrl.isEmpty() && currentChannelUrl.isNotEmpty()) {
            lastChannelUrl = currentChannelUrl
        }
    }
    
    LaunchedEffect(aspectRatio) {
        textureViewRef.value?.post {
            (textureViewRef.value as? IptvPlayerTextureView)?.updateAspectRatio(aspectRatio)
        }
    }
    
    LaunchedEffect(mirrorFlip) {
        textureViewRef.value?.post {
            (textureViewRef.value as? IptvPlayerTextureView)?.updateMirrorFlip(mirrorFlip)
        }
    }
    
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                textureViewRef.value?.post { 
                    textureViewRef.value?.requestLayout()
                }
            }
        }
        player?.exoPlayer?.addListener(listener)
    }
    
    DisposableEffect(player) {
        onDispose {
            player?.attachSurface(null)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (surfaceReady) {
            AndroidView(
                factory = { ctx ->
                    IptvPlayerTextureView(ctx, player, aspectRatio, mirrorFlip, onSurfaceReady).also {
                        textureViewRef.value = it
                    }
                },
                onRelease = {
                    player?.attachSurface(null)
                },
                modifier = Modifier
            )
        }
    }
}
