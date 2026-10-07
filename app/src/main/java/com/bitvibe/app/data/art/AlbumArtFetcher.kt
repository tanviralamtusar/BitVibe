package com.bitvibe.app.data.art

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Lets Coil load `bitvibe-art://` URIs (see [AlbumArt]) in any AsyncImage. */
class AlbumArtFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val requested = maxOf(
            options.size.width.pxOrElse { DEFAULT_SIZE_PX },
            options.size.height.pxOrElse { DEFAULT_SIZE_PX }
        )
        val bitmap = AlbumArt.load(context, uri, requested.coerceIn(MIN_SIZE_PX, MAX_SIZE_PX))
            // An error result lets the placeholder drawn underneath the image show through.
            ?: throw IOException("No album art for $uri")
        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (AlbumArt.isArtUri(data)) AlbumArtFetcher(context.applicationContext, data, options) else null
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 512
        const val MIN_SIZE_PX = 96
        const val MAX_SIZE_PX = 1024
    }
}
