package com.bitvibe.app.data.art

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

/**
 * Per-track album art.
 *
 * MediaStore's old `content://media/external/audio/albumart/<albumId>` URI is unreliable on
 * Android 10+ (often empty) and is shared by every song in an "album" — including the catch-all
 * album untagged singles land in. Instead each song gets a `bitvibe-art://audio/<id>` URI,
 * resolved here from the track's own embedded cover. Coil ([AlbumArtFetcher]) and the media
 * session ([AlbumArtBitmapLoader], for the notification and lock screen) both use it.
 */
object AlbumArt {
    const val SCHEME = "bitvibe-art"
    private const val QUERY_ALBUM = "album"
    private val LEGACY_ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    fun uriFor(audioId: Long, albumId: Long = 0L): Uri =
        Uri.Builder()
            .scheme(SCHEME)
            .authority("audio")
            .appendPath(audioId.toString())
            .apply { if (albumId > 0) appendQueryParameter(QUERY_ALBUM, albumId.toString()) }
            .build()

    fun isArtUri(uri: Uri?): Boolean = uri?.scheme == SCHEME

    /** Decodes the cover for an art URI, scaled to about [maxSizePx]. Null when the song has none. Blocking. */
    fun load(context: Context, uri: Uri, maxSizePx: Int): Bitmap? {
        val audioId = uri.lastPathSegment?.toLongOrNull() ?: return null
        val albumId = uri.getQueryParameter(QUERY_ALBUM)?.toLongOrNull()
        val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The system thumbnail is cached by MediaStore, so it's the fast path.
            loadThumbnail(context, audioUri, maxSizePx) ?: loadEmbedded(context, audioUri, maxSizePx)
        } else {
            loadEmbedded(context, audioUri, maxSizePx)
                ?: albumId?.let { loadLegacyAlbumArt(context, it, maxSizePx) }
        }
    }

    private fun loadThumbnail(context: Context, audioUri: Uri, maxSizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            context.contentResolver.loadThumbnail(audioUri, Size(maxSizePx, maxSizePx), null)
        } catch (e: Exception) {
            null // IOException when the file has no artwork
        }
    }

    private fun loadEmbedded(context: Context, audioUri: Uri, maxSizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            retriever.embeddedPicture?.let { decodeSampled(it, maxSizePx) }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun loadLegacyAlbumArt(context: Context, albumId: Long, maxSizePx: Int): Bitmap? = try {
        context.contentResolver
            .openInputStream(ContentUris.withAppendedId(LEGACY_ALBUM_ART_URI, albumId))
            ?.use { it.readBytes() }
            ?.let { decodeSampled(it, maxSizePx) }
    } catch (e: Exception) {
        null
    }

    /** Decodes [bytes] without allocating a full-size bitmap for large covers. */
    private fun decodeSampled(bytes: ByteArray, maxSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSizePx && bounds.outHeight / (sample * 2) >= maxSizePx) {
            sample *= 2
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
