package com.example.riffle

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
import androidx.compose.material3.Surface
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.riffle.ui.screen.ArticleReaderScreen
import com.example.riffle.ui.screen.HomeScreen
import com.example.riffle.ui.viewmodel.MainViewModel
import com.example.riffle.ui.theme.RiffleTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

import java.nio.charset.StandardCharsets
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

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
                    RiffleApp(viewModel)
                    
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
fun RiffleApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
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
                }
            )
        }
        
        composable(
            route = "reader/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
            popEnterTransition = { slideInHorizontally(tween(300)) { it } },
            popExitTransition = { slideOutHorizontally(tween(200)) { it } }
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            ArticleReaderScreen(
                url = url,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
