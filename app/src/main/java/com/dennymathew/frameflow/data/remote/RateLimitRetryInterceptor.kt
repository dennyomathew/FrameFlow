package com.dennymathew.frameflow.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries requests the server rejects with HTTP 429 (Too Many Requests).
 *
 * The Rick and Morty API rate-limits bursts, which a scrolling grid of avatars easily triggers.
 * Backs off exponentially from [baseDelayMillis], waiting longer when the server's `Retry-After`
 * header asks for more, and never more than [maxDelayMillis]. `Retry-After` is only a lower
 * bound: the API's Cloudflare front end sends `Retry-After: 0` with its 429s, and retrying
 * immediately would just be rejected again.
 */
class RateLimitRetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 10_000,
    private val sleep: (Long) -> Unit = Thread::sleep,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        var attempt = 0
        while (response.code == HTTP_TOO_MANY_REQUESTS && attempt < maxRetries) {
            val backoffMillis = baseDelayMillis shl attempt
            val delayMillis = maxOf(backoffMillis, retryAfterMillis(response) ?: 0)
            response.close()
            try {
                sleep(delayMillis.coerceAtMost(maxDelayMillis))
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting to retry", e)
            }
            attempt++
            response = chain.proceed(chain.request())
        }
        return response
    }

    private fun retryAfterMillis(response: Response): Long? =
        response.header("Retry-After")?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.times(1_000)

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
