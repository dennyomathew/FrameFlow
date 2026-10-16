package com.dennymathew.frameflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.dennymathew.frameflow.R
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.ui.CharacterDetailViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: Int,
    onBackClick: () -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val character by viewModel.character.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(characterId) {
        viewModel.load(characterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = character?.name ?: "Character") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (character != null) {
                        FavoriteButton(
                            isFavorite = isFavorite,
                            onClick = viewModel::toggleFavorite,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (character == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Character not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onBackClick) { Text("Back") }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(character?.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = character?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = character?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    character?.let { CharacterFacts(it) }
                }
            }
        }
    }
}

/** Status line plus labelled facts. Facts the API (or an old cached row) left empty are skipped. */
@Composable
private fun CharacterFacts(character: CharacterEntity) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        StatusDot(character.status)
        Text(
            text = listOf(character.status, character.species, character.type)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.titleMedium
        )
    }

    val facts = listOf(
        "Gender" to character.gender,
        "Origin" to character.originName,
        "Last known location" to character.locationName,
        "Episodes" to character.episodeCount.takeIf { it > 0 }?.toString().orEmpty(),
        "ID" to character.id.toString()
    ).filter { (_, value) -> value.isNotBlank() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            facts.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
