package com.bernardo.feedvault

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
class App : Application(), ImageLoaderFactory {

    companion object {
        // No GIF decoder — decodes only the first frame via BitmapFactory.
        // Use for grid/list thumbnails to avoid loading all frames of large GIFs into memory.
        lateinit var staticImageLoader: ImageLoader
            private set
    }

    override fun onCreate() {
        super.onCreate()
        com.bernardo.feedvault.vault.VaultSession.init(this)
        com.bernardo.feedvault.ui.theme.ThemeController.load(this)
        staticImageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.10)
                    .build()
            }
            .build()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .components {
                // GifDecoder (android-gif-drawable) implements Animatable — Coil auto-starts animation.
                // ImageDecoderDecoder returns AnimatedImageDrawable which does NOT implement Animatable,
                // so Coil's DrawablePainter never calls .start() and GIFs freeze on frame 0.
                // Add GifDecoder first so it wins for GIFs; ImageDecoderDecoder handles animated WebP/HEIF.
                add(GifDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Coil.imageLoader(this).memoryCache?.clear()
            staticImageLoader.memoryCache?.clear()
        }
    }
}
