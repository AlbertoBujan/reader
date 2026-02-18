package com.boaxente.riffle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import com.boaxente.riffle.data.remote.AuthManager
import javax.inject.Inject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.boaxente.riffle.ui.screen.ArticleReaderScreen
import com.boaxente.riffle.ui.screen.CommentsScreen
import com.boaxente.riffle.ui.screen.UserProfileScreen
import com.boaxente.riffle.ui.screen.HomeScreen
import com.boaxente.riffle.ui.viewmodel.MainViewModel
import com.boaxente.riffle.ui.theme.RiffleTheme
import com.boaxente.riffle.util.RiffleLogger
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

import java.nio.charset.StandardCharsets
import android.util.Base64
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive



import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.boaxente.riffle.worker.FeedSyncWorker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var logger: RiffleLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule background sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<FeedSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FeedSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        enableEdgeToEdge()

        logger.log("App Started")

        // State for update dialog
        val updateInfo = mutableStateOf<Update?>(null)
        val isDownloading = mutableStateOf(false)
        val downloadProgress = mutableFloatStateOf(-1f)
        val currentDownloadId = mutableLongStateOf(-1L)

        // Check for updates manually
        val appUpdaterUtils = AppUpdaterUtils(this)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("AlbertoBujan", "reader")
            .withListener(object : com.github.javiersantos.appupdater.AppUpdaterUtils.UpdateListener {
                override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                    if (isUpdateAvailable == true) {
                        updateInfo.value = update
                    }
                }

                override fun onFailed(error: AppUpdaterError?) {
                    // Handle error if needed
                }
            })
        
        appUpdaterUtils.start()
        
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { isDarkMode }
                )
            }

            LaunchedEffect(isDownloading.value, currentDownloadId.longValue) {
                if (isDownloading.value && currentDownloadId.longValue != -1L) {
                    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    while (isActive && isDownloading.value) {
                        val query = DownloadManager.Query().setFilterById(currentDownloadId.longValue)
                        val cursor: Cursor = manager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                            
                            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            
                            if (bytesDownloadedIndex != -1 && totalBytesIndex != -1) {
                                val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                                val totalBytes = cursor.getLong(totalBytesIndex)
                                if (totalBytes > 0) {
                                    downloadProgress.floatValue = bytesDownloaded.toFloat() / totalBytes.toFloat()
                                } else {
                                    downloadProgress.floatValue = -1f
                                }
                            }

                            if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                                isDownloading.value = false
                                updateInfo.value = null // Close dialog
                            }
                        }
                        cursor.close()
                        delay(100) // Poll every 100ms
                    }
                }
            }

            RiffleTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RiffleApp(viewModel, authManager)
                    
                    // Show custom update dialog
                    updateInfo.value?.let { update ->
                            AlertDialog(
                                onDismissRequest = { 
                                    if (!isDownloading.value) updateInfo.value = null 
                                },
                                title = { 
                                    Text(text = if (isDownloading.value) stringResource(R.string.update_download_title) else stringResource(R.string.update_dialog_title)) 
                                },
                                text = { 
                                    if (isDownloading.value) {
                                        androidx.compose.foundation.layout.Column {
                                            Text(text = stringResource(R.string.update_download_desc))
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                            
                                            if (downloadProgress.floatValue >= 0f) {
                                                LinearProgressIndicator(
                                                    progress = { downloadProgress.floatValue },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${(downloadProgress.floatValue * 100).toInt()}%",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                    } else {
                                        Text(text = stringResource(R.string.update_dialog_message, update.latestVersion)) 
                                    }
                                },
                                confirmButton = {
                                    if (!isDownloading.value) {
                                        TextButton(
                                            onClick = { 
                                                // Usamos la URL directa genérica
                                                val id = downloadAndInstall("https://github.com/AlbertoBujan/reader/releases/latest/download/Riffle.apk")
                                                currentDownloadId.longValue = id
                                                isDownloading.value = true
                                            }
                                        ) {
                                            Text(stringResource(R.string.update_button_now))
                                        }
                                    }
                                },
                                dismissButton = {
                                    if (!isDownloading.value) {
                                        TextButton(onClick = { updateInfo.value = null }) {
                                            Text(stringResource(R.string.update_button_later))
                                        }
                                    }
                                }
                            )
                    }
                }
            }
        }
    }

    companion object {
        private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }

    private fun downloadAndInstall(url: String): Long {
        // 1. Configurar dónde se guardará el APK
        val file = File(externalCacheDir, "update.apk")
        if (file.exists()) file.delete() // Borrar si había uno previo

        // 2. Usar el DownloadManager de Android
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(getString(R.string.update_download_title))
            .setDescription(getString(R.string.update_download_desc))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        // 3. Escuchar cuando termine la descarga
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val installIntent = Intent(Intent.ACTION_VIEW)
                val apkUri = FileProvider.getUriForFile(
                    context, 
                    "${packageName}.fileprovider", 
                    file
                )
                
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                try {
                    startActivity(installIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                unregisterReceiver(this)
            }
        }
        // Register receiver without separate flags for now as it captures system broadcast
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        return downloadId
    }
}

@Composable
fun RiffleApp(viewModel: MainViewModel, authManager: AuthManager) {
    val navController = rememberNavController()
    val currentUser by authManager.currentUser.collectAsState()
    
    // Choose start destination based on auth state (or preferences, if we allowed skip)
    // For now simple: if no user -> login. 
    // Ideally we might check preference "hasSkippedLogin" too.
    val startDest = if (currentUser == null) "login" else "home"
    
    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            com.boaxente.riffle.ui.screen.LoginScreen(
                authManager = authManager,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSkip = {
                     navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = "home",
            enterTransition = { slideInHorizontally(tween(300)) { -it } },
            exitTransition = { slideOutHorizontally(tween(300)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(200)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onArticleClick = { link, _ ->
                     val encodedUrl = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                     navController.navigate("reader/$encodedUrl")
                },
                onNavigateToFeedSearch = {
                    navController.navigate("search_feed")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable(
            route = "search_feed",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) {
            com.boaxente.riffle.ui.screen.SearchFeedScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPreview = { url, title, icon ->
                    val encodedUrl = Base64.encodeToString(url.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                    val encodedTitle = if (title != null) Base64.encodeToString(title.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP) else "null"
                    val encodedIcon = if (icon != null) Base64.encodeToString(icon.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP) else "null"
                    navController.navigate("feed_preview/$encodedUrl/$encodedTitle/$encodedIcon")
                },
                viewModel = viewModel
            )
        }

        composable(
            route = "feed_preview/{feedUrl}/{feedTitle}/{feedIconUrl}",
            arguments = listOf(
                navArgument("feedUrl") { type = NavType.StringType },
                navArgument("feedTitle") { type = NavType.StringType },
                navArgument("feedIconUrl") { type = NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("feedUrl") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("feedTitle") ?: "null"
            val encodedIcon = backStackEntry.arguments?.getString("feedIconUrl") ?: "null"

            val url = String(Base64.decode(encodedUrl, Base64.URL_SAFE), StandardCharsets.UTF_8)
            val title = if (encodedTitle != "null") String(Base64.decode(encodedTitle, Base64.URL_SAFE), StandardCharsets.UTF_8) else null
            val icon = if (encodedIcon != "null") String(Base64.decode(encodedIcon, Base64.URL_SAFE), StandardCharsets.UTF_8) else null

            com.boaxente.riffle.ui.screen.FeedPreviewScreen(
                feedUrl = url,
                feedTitle = title,
                feedIconUrl = icon,
                onBack = { navController.popBackStack() },
                onArticleClick = { link, _ ->
                     val encodedArticleUrl = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                     val encodedSourceTitle = if (title != null) Base64.encodeToString(title.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP) else ""
                     navController.navigate("reader/$encodedArticleUrl?isPreview=true&sourceTitle=$encodedSourceTitle")
                },
                onAddFeed = {
                    viewModel.addSource(url, title, icon)
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = "reader/{url}?isPreview={isPreview}&sourceTitle={sourceTitle}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("isPreview") { defaultValue = false; type = NavType.BoolType },
                navArgument("sourceTitle") { defaultValue = ""; type = NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val isPreview = backStackEntry.arguments?.getBoolean("isPreview") ?: false
            val sourceTitleArg = backStackEntry.arguments?.getString("sourceTitle") ?: ""
            val sourceTitle = if (sourceTitleArg.isNotEmpty()) String(Base64.decode(sourceTitleArg, Base64.URL_SAFE), StandardCharsets.UTF_8) else null
            
            ArticleReaderScreen(
                url = url,
                isPreview = isPreview,
                sourceTitle = sourceTitle,
                onBack = { navController.popBackStack() },
                onCommentsClick = { articleLink, articleTitle ->
                    val encodedLink = Base64.encodeToString(articleLink.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                    val encodedTitle = Base64.encodeToString(articleTitle.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                    navController.navigate("comments/$encodedLink/$encodedTitle")
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = "comments/{articleLink}/{articleTitle}",
            arguments = listOf(
                navArgument("articleLink") { type = NavType.StringType },
                navArgument("articleTitle") { type = NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) { backStackEntry ->
            val articleLink = try {
                String(Base64.decode(
                    backStackEntry.arguments?.getString("articleLink") ?: "",
                    Base64.URL_SAFE
                ), StandardCharsets.UTF_8)
            } catch (e: Exception) { "" }
            val articleTitle = try {
                String(Base64.decode(
                    backStackEntry.arguments?.getString("articleTitle") ?: "",
                    Base64.URL_SAFE
                ), StandardCharsets.UTF_8)
            } catch (e: Exception) { "" }
            CommentsScreen(
                articleLink = articleLink,
                articleTitle = articleTitle,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "profile",
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) {
            UserProfileScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { articleLink ->
                    val encodedUrl = URLEncoder.encode(articleLink, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encodedUrl")
                }
            )
        }
    }
}
