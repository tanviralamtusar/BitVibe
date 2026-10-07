package com.bitvibe.app.di

import android.content.Context
import com.bitvibe.app.BitVibeApp
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: com.bitvibe.app.data.repository.MediaRepositoryImpl
    ): com.bitvibe.app.domain.repository.MediaRepository

    @Binds
    @Singleton
    abstract fun bindMusicController(
        musicControllerImpl: com.bitvibe.app.data.player.MusicControllerImpl
    ): com.bitvibe.app.domain.player.MusicController

    companion object {
        @Provides
        @Singleton
        fun provideApplication(@ApplicationContext app: Context): BitVibeApp {
            return app as BitVibeApp
        }
        
        @Provides
        @Singleton
        fun provideExoPlayer(@ApplicationContext context: Context): androidx.media3.exoplayer.ExoPlayer {
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build()
                
            return androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
        }

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): com.bitvibe.app.data.local.AppDatabase {
            return androidx.room.Room.databaseBuilder(
                context,
                com.bitvibe.app.data.local.AppDatabase::class.java,
                "bitvibe_db"
            ).addMigrations(com.bitvibe.app.data.youtube.MIGRATION_1_2).build()
        }

        @Provides
        @Singleton
        fun providePlaylistDao(db: com.bitvibe.app.data.local.AppDatabase): com.bitvibe.app.data.local.PlaylistDao {
            return db.playlistDao()
        }

        @Provides
        @Singleton
        fun provideYouTubeFavoriteDao(db: com.bitvibe.app.data.local.AppDatabase): com.bitvibe.app.data.youtube.YouTubeFavoriteDao {
            return db.youTubeFavoriteDao()
        }
    }
}
