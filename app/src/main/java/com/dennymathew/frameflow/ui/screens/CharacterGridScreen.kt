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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import android.util.Log
import coil.request.ImageRequest
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.dennymathew.frameflow.R
import com.dennymathew.frameflow.data.CharacterFilter
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
    var showFavorites by rememberSaveable { mutableStateOf(false) }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var speciesFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var genderFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = CharacterFilter(statusFilter, speciesFilter, genderFilter)
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isSearching = query.isNotBlank() || filter.isActive
    val isSearchSyncing by viewModel.isSearchSyncing.collectAsState(initial = false)
    LaunchedEffect(query) {
        viewModel.setSearchQuery(query)
    }
    LaunchedEffect(filter) {
        viewModel.setFilter(filter)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                ) {
                    FilterDropdownChip(
                        label = "Status",
                        options = CharacterFilter.STATUSES,
                        selected = statusFilter,
                        onSelect = { statusFilter = it }
                    )
                    FilterDropdownChip(
                        label = "Species",
                        options = CharacterFilter.SPECIES,
                        selected = speciesFilter,
                        onSelect = { speciesFilter = it }
                    )
                    FilterDropdownChip(
                        label = "Gender",
                        options = CharacterFilter.GENDERS,
                        selected = genderFilter,
                        onSelect = { genderFilter = it }
                    )
                    if (filter.isActive) {
                        TextButton(onClick = {
                            statusFilter = null
                            speciesFilter = null
                            genderFilter = null
                        }) { Text("Clear filters") }
                    }
                }
                ListToggle(
                    showFavorites = showFavorites,
                    onShowFavoritesChange = { showFavorites = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                if (showFavorites) {
                    // Favorites are stored on the device, so connection status doesn't apply.
                } else if (!isOnline) {
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
        // safeDrawing includes the keyboard, so search results can scroll above it.
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Edge-to-edge: the container takes only the top bar's space; grids get the bottom and
        // side insets as contentPadding so they scroll behind the navigation bar.
        val layoutDirection = LocalLayoutDirection.current
        val gridPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
            top = 12.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
            bottom = innerPadding.calculateBottomPadding() + 12.dp
        )
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .consumeWindowInsets(innerPadding)
        if (showFavorites) {
            FavoritesGrid(
                favorites = favorites.filter {
                    it.name.contains(query.trim(), ignoreCase = true) && filter.matches(it)
                },
                isFiltered = query.isNotBlank() || filter.isActive,
                onCharacterClick = onCharacterClick,
                onToggleFavorite = viewModel::toggleFavorite,
                contentPadding = gridPadding,
                modifier = contentModifier
            )
            return@Scaffold
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                lazyPagingItems.refresh()
                if (isSearching) {
                    viewModel.refreshSearch()
                }
            },
            modifier = contentModifier
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
                                text = if (query.isNotBlank()) {
                                    "No characters found for \"${query.trim()}\""
                                } else {
                                    "No characters match these filters"
                                },
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
                        contentPadding = gridPadding,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lazyPagingItems.itemCount) { index ->
                            val character = lazyPagingItems[index]
                            if (character != null) {
                                CharacterCard(
                                    character = character,
                                    isFavorite = character.id in favoriteIds,
                                    onClick = { onCharacterClick(character.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(character) }
                                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListToggle(
    showFavorites: Boolean,
    onShowFavoritesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = !showFavorites,
            onClick = { onShowFavoritesChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text("All") }
        SegmentedButton(
            selected = showFavorites,
            onClick = { onShowFavoritesChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text("Favorites") }
    }
}

@Composable
fun FavoritesGrid(
    favorites: List<CharacterEntity>,
    isFiltered: Boolean,
    onCharacterClick: (Int) -> Unit,
    onToggleFavorite: (CharacterEntity) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (favorites.isEmpty()) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (isFiltered) {
                    "No favorites match your search"
                } else {
                    "No favorites yet. Tap ♡ on a character to save it."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(favorites.size, key = { favorites[it].id }) { index ->
            val character = favorites[index]
            CharacterCard(
                character = character,
                isFavorite = true,
                onClick = { onCharacterClick(character.id) },
                onToggleFavorite = { onToggleFavorite(character) }
            )
        }
    }
}

/** A heart toggle; text glyphs avoid pulling in the Material icons library. */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
        }
    ) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.titleLarge,
            color = if (isFavorite) Color(0xFFE91E63) else Color.White,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
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
                    StatusDot(character.status)

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

/** Green for alive, red for dead, grey for unknown. */
@Composable
internal fun StatusDot(status: String, modifier: Modifier = Modifier) {
    val color = when (status.lowercase()) {
        "alive" -> Color(0xFF4CAF50)
        "dead" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * A filter chip that opens a menu of [options]. Shows [label] when nothing is selected and the
 * selected option otherwise; "Any" clears it.
 */
@Composable
private fun FilterDropdownChip(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let(::filterLabel) ?: label) },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any ${label.lowercase()}") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(filterLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** API values are lower case ("alive", "unknown"); show them capitalised. */
private fun filterLabel(value: String) = value.replaceFirstChar { it.uppercase() }
