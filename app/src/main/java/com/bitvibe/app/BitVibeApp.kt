package com.bitvibe.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.bitvibe.app.data.art.AlbumArtFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BitVibeApp : Application(), ImageLoaderFactory {

    // Every AsyncImage uses this loader; it adds per-track album art (`bitvibe-art://` URIs).
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(AlbumArtFetcher.Factory(this@BitVibeApp)) }
            .crossfade(true)
            .build()
}
