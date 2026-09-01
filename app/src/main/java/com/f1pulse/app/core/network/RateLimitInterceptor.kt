package com.f1pulse.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Simple blocking token-bucket rate limiter shared per API host.
 *
 * Providers publish limits on more than one time scale — OpenF1's free tier allows 3 requests
 * per second *and* 30 per minute — so a bucket can carry a secondary [perMinute] cap.
 * A per-second bucket alone would permit 180 requests in a minute.
 */
class TokenBucket(
    private val ratePerSecond: Double,
    private val perMinute: Int? = null,
) {
    private val capacity: Int = ratePerSecond.toInt().coerceAtLeast(1)
    private var tokens: Double = capacity.toDouble()
    private var lastRefillNanos: Long = System.nanoTime()

    /** Completion times of requests issued in the trailing minute, oldest first. */
    private val recentRequestNanos = ArrayDeque<Long>()

    @Synchronized
    fun acquire() {
        while (true) {
            val minuteWaitMs = minuteWaitMillis()
            if (minuteWaitMs > 0) {
                await(minuteWaitMs)
                continue
            }
            refill()
            if (tokens >= 1.0) {
                tokens -= 1.0
                if (perMinute != null) recentRequestNanos.addLast(System.nanoTime())
                return
            }
            val deficit = 1.0 - tokens
            await((deficit / ratePerSecond * 1000.0).toLong())
        }
    }

    /**
     * Waits without discarding the thread's interrupt state — swallowing it would let a
     * cancelled OkHttp call spin here instead of unwinding.
     */
    private fun await(millis: Long) {
        try {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as Object).wait(millis.coerceAtLeast(1L))
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw interrupted
        }
    }

    /** Milliseconds until the trailing-minute quota frees a slot; 0 when one is available. */
    private fun minuteWaitMillis(): Long {
        val limit = perMinute ?: return 0L
        val now = System.nanoTime()
        val windowStart = now - TimeUnit.MINUTES.toNanos(1)
        while (recentRequestNanos.isNotEmpty() && recentRequestNanos.first() < windowStart) {
            recentRequestNanos.removeFirst()
        }
        if (recentRequestNanos.size < limit) return 0L
        val oldest = recentRequestNanos.first()
        return TimeUnit.NANOSECONDS.toMillis(oldest - windowStart).coerceAtLeast(1L)
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsedSec = (now - lastRefillNanos) / 1_000_000_000.0
        if (elapsedSec > 0) {
            tokens = (tokens + elapsedSec * ratePerSecond).coerceAtMost(capacity.toDouble())
            lastRefillNanos = now
        }
    }
}

/**
 * OkHttp interceptor that throttles each host with its own [TokenBucket] and honours HTTP 429
 * `Retry-After` by sleeping before retrying (up to 3 times).
 *
 * An interceptor sees every request regardless of host, so the buckets are keyed by a host
 * fragment and looked up per call — the two APIs have different limits and must not share a
 * budget.
 */
class RateLimitInterceptor(
    private val bucketsByHostFragment: Map<String, TokenBucket>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response = proceedOnce(chain)
        while (response.code == 429 && attempt < 3) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 1L
            response.close()
            TimeUnit.SECONDS.sleep(retryAfter.coerceAtMost(5L))
            attempt++
            response = proceedOnce(chain)
        }
        return response
    }

    private fun proceedOnce(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        bucketsByHostFragment.entries
            .firstOrNull { (fragment, _) -> host.contains(fragment) }
            ?.value
            ?.acquire()
        return chain.proceed(chain.request())
    }
}
