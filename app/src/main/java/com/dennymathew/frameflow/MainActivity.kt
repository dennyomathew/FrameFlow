package com.dennymathew.frameflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dennymathew.frameflow.ui.CharacterDetailViewModel
import com.dennymathew.frameflow.ui.CharacterViewModel
import com.dennymathew.frameflow.ui.screens.CharacterDetailScreen
import com.dennymathew.frameflow.ui.screens.CharacterGridScreen
import com.dennymathew.frameflow.ui.theme.FrameFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrameFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: CharacterViewModel = hiltViewModel()
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "grid") {
                        composable("grid") {
                            CharacterGridScreen(viewModel = viewModel, onCharacterClick = { id ->
                                navController.navigate("detail/$id")
                            })
                        }
                        composable(
                            route = "detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val idArg = backStackEntry.arguments?.getInt("id") ?: return@composable
                            val detailViewModel: CharacterDetailViewModel = hiltViewModel()
                            CharacterDetailScreen(
                                characterId = idArg,
                                viewModel = detailViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
