package com.dennymathew.frameflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import coil.request.ImageRequest
import coil.compose.SubcomposeAsyncImage
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.ui.CharacterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterGridScreen(
    viewModel: CharacterViewModel,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val isSearching = query.isNotBlank()
    val isSearchSyncing by viewModel.isSearchSyncing.collectAsState(initial = false)
    LaunchedEffect(query) {
        viewModel.setSearchQuery(query)
    }

    val lazyPagingItems: LazyPagingItems<CharacterEntity> =
        if (isSearching) viewModel.searchFlow.collectAsLazyPagingItems()
        else viewModel.charactersFlow.collectAsLazyPagingItems()

    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount > 0
    val searchSyncFailed by viewModel.searchSyncFailed.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    // Loads that failed while offline are retried automatically once the connection returns.
    LaunchedEffect(isOnline) {
        val loadState = lazyPagingItems.loadState
        if (isOnline && (loadState.refresh is LoadState.Error || loadState.append is LoadState.Error)) {
            lazyPagingItems.retry()
        }
    }

    // Online failures (server errors, timeouts) can succeed on a manual retry, so offer one.
    // Offline failures are covered by the offline banner and the auto-retry above.
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshError = (lazyPagingItems.loadState.refresh as? LoadState.Error)?.error
    val hasItems = lazyPagingItems.itemCount > 0
    LaunchedEffect(refreshError, hasItems, isOnline) {
        if (refreshError == null || !hasItems || !isOnline) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Couldn't refresh characters.",
            actionLabel = "Retry",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) lazyPagingItems.retry()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Rick & Morty Portal",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Search by character name") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            TextButton(onClick = { query = "" }) { Text("Clear") }
                        }
                    }
                )
                if (!isOnline) {
                    Text(
                        text = "Offline: showing saved characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                } else if (isSearching && isSearchSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isSearching && searchSyncFailed) {
                    Text(
                        text = "Couldn't update results: showing saved matches only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                lazyPagingItems.refresh()
                if (isSearching) {
                    viewModel.refreshSearch()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val refreshState = lazyPagingItems.loadState.refresh
            when {
                // Only take over the screen when there is nothing cached to show.
                lazyPagingItems.itemCount == 0 && refreshState is LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                lazyPagingItems.itemCount == 0 && refreshState is LoadState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isOnline) "Failed to load characters" else "You're offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isOnline) {
                                refreshState.error.localizedMessage ?: "Unknown error"
                            } else {
                                "Characters will load when you reconnect."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOnline) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { lazyPagingItems.retry() }) {
                                Text(text = "Retry")
                            }
                        }
                    }
                }
                isSearching && lazyPagingItems.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearchSyncing) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(
                                text = "No characters found for \"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                else -> {
                    // Character grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lazyPagingItems.itemCount) { index ->
                            val character = lazyPagingItems[index]
                            if (character != null) {
                                CharacterCard(character = character, onClick = { onCharacterClick(character.id) })
                            }
                        }

                        // Bottom loaders & error handlers for append state
                        when (val appendState = lazyPagingItems.loadState.append) {
                            is LoadState.Loading -> {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            }
                            is LoadState.Error -> {
                                item(span = { GridItemSpan(2) }) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (!isOnline) {
                                            Text(
                                                text = "More characters will load when you reconnect",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                            return@Column
                                        }
                                        Text(
                                            text = "Failed to load more characters",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { lazyPagingItems.retry() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Text(text = "Retry", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(character.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = character.name,
                    contentScale = ContentScale.Crop,
                    onError = { state ->
                        Log.e("FrameFlow", "Coil failed to load: ${character.imageUrl}", state.result.throwable)
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "❌",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Character details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusColor = when (character.status.lowercase()) {
                        "alive" -> Color(0xFF4CAF50)
                        "dead" -> Color(0xFFF44336)
                        else -> Color(0xFF9E9E9E)
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Text(
                        text = "${character.status} - ${character.species}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
