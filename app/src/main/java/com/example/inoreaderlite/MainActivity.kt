package com.example.inoreaderlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.inoreaderlite.ui.screen.ArticleReaderScreen
import com.example.inoreaderlite.ui.screen.HomeScreen
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InoreaderLiteMain()
        }
    }
}

@Composable
fun InoreaderLiteMain() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onArticleClick = { link ->
                     val encodedUrl = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                     navController.navigate("reader/$encodedUrl")
                }
            )
        }
        
        composable(
            route = "reader/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            ArticleReaderScreen(
                url = url,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
