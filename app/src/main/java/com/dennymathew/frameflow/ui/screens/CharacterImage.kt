package com.dennymathew.frameflow.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dennymathew.frameflow.R

/**
 * Whether the device is online, provided by `MainActivity` from `ConnectivityObserver`.
 * Defaults to online so previews and tests that don't provide it behave normally.
 */
val LocalIsOnline = compositionLocalOf { true }

/**
 * A character's image with a loading spinner and, if loading fails, a way to recover:
 * - online (usually a 429 that outlasted `RateLimitRetryInterceptor`): a Retry button;
 * - offline (Coil only reads its disk cache then, and fails with a 504 "only-if-cached" for
 *   images it hasn't cached): an "Offline" note, and the image reloads by itself once the
 *   connection returns.
 */
@Composable
fun CharacterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val isOnline = LocalIsOnline.current
    val currentIsOnline by rememberUpdatedState(isOnline)
    var retryCount by remember(url) { mutableIntStateOf(0) }
    var failed by remember(url) { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(!isOnline) }

    // Reload a failed image only when the connection comes back (offline -> online), not on
    // the first composition, so an online failure keeps showing Retry.
    LaunchedEffect(isOnline) {
        if (!isOnline) {
            wasOffline = true
        } else if (wasOffline) {
            wasOffline = false
            if (failed) retryCount++
        }
    }

    // A new key recreates the image and so starts a new request. Coil doesn't cache failures,
    // so that request goes back to the network.
    key(retryCount) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onLoading = { failed = false },
            onSuccess = { failed = false },
            onError = { state ->
                failed = true
                if (currentIsOnline) {
                    Log.e("FrameFlow", "Coil failed to load: $url", state.result.throwable)
                } else {
                    // Expected: offline, Coil only serves images it has cached.
                    Log.w("FrameFlow", "Image not cached while offline: $url")
                }
            },
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                ImageLoadError(isOnline = isOnline, onRetry = { retryCount++ })
            },
            modifier = modifier
        )
    }
}

@Composable
private fun ImageLoadError(isOnline: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isOnline) {
            // Retrying can't work offline; the image reloads when the connection returns.
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Image loads when you reconnect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            return@Column
        }
        Text(
            text = "Couldn't load image",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        FilledTonalButton(
            onClick = onRetry,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = "Retry")
        }
    }
}
