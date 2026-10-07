package com.bitvibe.app.data.youtube

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.text.HtmlCompat
import com.bitvibe.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val channelId: String,
    val thumbnailUrl: String
)

data class YouTubeSearchPage(val videos: List<YouTubeVideo>, val nextPageToken: String?)

class YouTubeApiException(message: String) : Exception(message)

/**
 * Search through the official YouTube Data API v3. Each search costs 100 of the default
 * 10,000 daily quota units (about 100 searches a day per key).
 *
 * Requests carry the app's package name and signing-certificate SHA-1, so the key can be
 * restricted to BeatVibe in Google Cloud Console (Credentials → Android apps).
 */
@Singleton
class YouTubeApi @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isConfigured: Boolean get() = BuildConfig.YOUTUBE_API_KEY.isNotBlank()

    private val certSha1: String? by lazy { signingCertSha1() }

    /** Music videos matching [query], or uploads from [channelId] when given. */
    suspend fun search(
        query: String,
        channelId: String? = null,
        pageToken: String? = null
    ): YouTubeSearchPage = withContext(Dispatchers.IO) {
        if (!isConfigured) throw YouTubeApiException("YouTube search isn't set up (no API key in this build).")
        val params = buildMap {
            put("part", "snippet")
            put("type", "video")
            put("videoEmbeddable", "true") // only videos the official player is allowed to show
            put("maxResults", "25")
            put("safeSearch", "moderate")
            if (channelId != null) {
                put("channelId", channelId)
                put("order", "viewCount")
            } else {
                put("videoCategoryId", "10") // Music
            }
            if (query.isNotBlank()) put("q", query)
            if (pageToken != null) put("pageToken", pageToken)
            put("key", BuildConfig.YOUTUBE_API_KEY)
        }
        val url = "https://www.googleapis.com/youtube/v3/search?" +
            params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Android-Package", context.packageName)
            certSha1?.let { setRequestProperty("X-Android-Cert", it) }
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw YouTubeApiException(errorMessage(code, body))
            parse(JSONObject(body))
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: JSONObject): YouTubeSearchPage {
        val items = json.optJSONArray("items")
        val videos = (0 until (items?.length() ?: 0)).mapNotNull { i ->
            val item = items!!.getJSONObject(i)
            val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            val thumbs = snippet.optJSONObject("thumbnails")
            val thumb = listOf("high", "medium", "default")
                .firstNotNullOfOrNull { thumbs?.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank) }
                ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            YouTubeVideo(
                id = id,
                // The API returns HTML-escaped text (&amp;, &#39; …).
                title = unescape(snippet.optString("title")),
                channelTitle = unescape(snippet.optString("channelTitle")),
                channelId = snippet.optString("channelId"),
                thumbnailUrl = thumb
            )
        }
        return YouTubeSearchPage(videos, json.optString("nextPageToken").takeIf { it.isNotBlank() })
    }

    private fun errorMessage(code: Int, body: String): String {
        val reason = runCatching {
            JSONObject(body).getJSONObject("error").getJSONArray("errors").getJSONObject(0).optString("reason")
        }.getOrNull()
        return when (reason) {
            "quotaExceeded", "dailyLimitExceeded" -> "Today's YouTube search limit is used up. It resets at midnight Pacific time."
            "keyInvalid", "API_KEY_INVALID" -> "The YouTube API key in this build isn't valid."
            "forbidden", "ipRefererBlocked", "accessNotConfigured" ->
                "The YouTube API key doesn't allow this app. Check its restrictions in Google Cloud Console."
            else -> "YouTube search failed (HTTP $code)."
        }
    }

    private fun unescape(text: String): String =
        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    @Suppress("DEPRECATION")
    private fun signingCertSha1(): String? = try {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        signatures?.firstOrNull()?.toByteArray()?.let { cert ->
            MessageDigest.getInstance("SHA-1").digest(cert).joinToString("") { "%02X".format(it) }
        }
    } catch (e: Exception) {
        null
    }
}
