package com.bitvibe.app.data.art

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Media session artwork for the notification, lock screen and connected devices.
 * `bitvibe-art://` URIs go through [AlbumArt]; anything else uses Media3's default loader.
 */
@OptIn(UnstableApi::class)
class AlbumArtBitmapLoader(context: Context) : BitmapLoader {
    private val appContext = context.applicationContext
    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    private val fallback = DataSourceBitmapLoader(appContext)

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        if (!AlbumArt.isArtUri(uri)) return fallback.loadBitmap(uri)
        return executor.submit<Bitmap> {
            AlbumArt.load(appContext, uri, SESSION_ART_SIZE_PX) ?: throw IOException("No album art for $uri")
        }
    }

    private companion object {
        const val SESSION_ART_SIZE_PX = 512
    }
}
