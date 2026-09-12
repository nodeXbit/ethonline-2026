package io.github.nodexbit.ethonline2026

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object PassArtworkDisplayPolicy {
    fun useFallback(source: PassArtworkSource, bitmapAvailable: Boolean): Boolean =
        source == PassArtworkSource.Fallback || !bitmapAvailable
}

class PassArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(url: String): Bitmap? = cache.get(url)

    fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        val bytes = download(url) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        if (bounds.outWidth.toLong() * bounds.outHeight.toLong() > MAX_SOURCE_PIXELS) return null
        var sampleSize = 1
        while ((bounds.outWidth / sampleSize).toLong() * (bounds.outHeight / sampleSize) > TARGET_PIXELS) {
            sampleSize *= 2
        }
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            )
        }.getOrNull() ?: return null
        cache.put(url, bitmap)
        return bitmap
    }

    private fun download(initialUrl: String): ByteArray? {
        var next = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val safe = PassArtworkPolicy.source(next) as? PassArtworkSource.Remote ?: return null
            val connection = (URL(safe.url).openConnection() as? HttpURLConnection) ?: return null
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.useCaches = true
                connection.setRequestProperty("Accept", "image/*")
                connection.connect()
                if (connection.responseCode in 300..399) {
                    if (redirectCount == MAX_REDIRECTS) return null
                    val location = connection.getHeaderField("Location") ?: return null
                    next = runCatching { URL(URL(safe.url), location).toString() }.getOrNull() ?: return null
                    return@repeat
                }
                if (connection.responseCode !in 200..299) return null
                if (!connection.contentType.orEmpty().substringBefore(';').trim().lowercase().startsWith("image/")) return null
                if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) return null
                return connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_DOWNLOAD_BYTES) return null
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                }
            } catch (_: Throwable) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 7_000
        const val MAX_DOWNLOAD_BYTES = 5 * 1024 * 1024
        const val MAX_SOURCE_PIXELS = 16_000_000L
        const val TARGET_PIXELS = 2_500_000L
        const val MAX_REDIRECTS = 2
        const val CACHE_BYTES = 8 * 1024 * 1024
    }
}
