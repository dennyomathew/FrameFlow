package com.dennymathew.frameflow.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitRetryInterceptorTest {

    private val sleeps = mutableListOf<Long>()
    private var calls = 0

    /** Serves [responses] in order (the last one repeats) instead of touching the network. */
    private fun client(vararg responses: Pair<Int, String?>): OkHttpClient {
        val server = Interceptor { chain ->
            val (code, retryAfter) = responses[minOf(calls++, responses.lastIndex)]
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .apply { retryAfter?.let { header("Retry-After", it) } }
                .body("".toResponseBody())
                .build()
        }
        return OkHttpClient.Builder()
            .addInterceptor(RateLimitRetryInterceptor(sleep = { sleeps += it }))
            .addInterceptor(server)
            .build()
    }

    private fun OkHttpClient.get(): Int =
        newCall(Request.Builder().url("https://example.com/avatar/1.jpeg").build())
            .execute().use { it.code }

    @Test
    fun successIsNotRetried() {
        assertEquals(200, client(200 to null).get())
        assertEquals(1, calls)
        assertEquals(emptyList<Long>(), sleeps)
    }

    @Test
    fun rateLimitedRequestIsRetriedWithExponentialBackoff() {
        assertEquals(200, client(429 to null, 429 to null, 200 to null).get())
        assertEquals(3, calls)
        assertEquals(listOf(1_000L, 2_000L), sleeps)
    }

    @Test
    fun retryAfterHeaderIsHonouredAndCapped() {
        assertEquals(200, client(429 to "2", 429 to "60", 200 to null).get())
        assertEquals(listOf(2_000L, 10_000L), sleeps)
    }

    @Test
    fun givesUpAfterMaxRetries() {
        assertEquals(429, client(429 to null).get())
        assertEquals(4, calls)
        assertEquals(listOf(1_000L, 2_000L, 4_000L), sleeps)
    }

    @Test
    fun otherErrorsAreNotRetried() {
        assertEquals(404, client(404 to null).get())
        assertEquals(1, calls)
    }
}
