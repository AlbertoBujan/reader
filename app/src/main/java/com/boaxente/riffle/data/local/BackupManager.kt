package com.boaxente.riffle.data.local

import com.boaxente.riffle.data.local.dao.FeedDao
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val feedDao: FeedDao,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {

    suspend fun exportBackup(outputStream: OutputStream) {
        withContext(Dispatchers.IO) {
            val sources = feedDao.getAllSourcesList()
            val folders = feedDao.getAllFoldersList()
            val savedArticles = feedDao.getSavedArticlesList()

            val preferences = mapOf(
                "isDarkMode" to preferencesManager.isDarkMode(),
                "markAsReadOnScroll" to preferencesManager.isMarkAsReadOnScroll(),
                "geminiApiKey" to preferencesManager.getGeminiApiKey(),
                "language" to preferencesManager.getLanguage(),
                "syncInterval" to preferencesManager.getSyncInterval()
            )

            val backupData = BackupData(
                timestamp = System.currentTimeMillis(),
                preferences = preferences,
                folders = folders,
                sources = sources,
                savedArticles = savedArticles
            )

            val jsonString = gson.toJson(backupData)
            outputStream.use { it.write(jsonString.toByteArray()) }
        }
    }

    suspend fun importBackup(inputStream: InputStream) {
        withContext(Dispatchers.IO) {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val backupData = gson.fromJson(jsonString, BackupData::class.java)

            // Restore Preferences
            backupData.preferences["isDarkMode"]?.let { preferencesManager.setDarkMode(it as Boolean) }
            backupData.preferences["markAsReadOnScroll"]?.let { preferencesManager.setMarkAsReadOnScroll(it as Boolean) }
            backupData.preferences["geminiApiKey"]?.let { preferencesManager.setGeminiApiKey(it as String) }
            backupData.preferences["language"]?.let { preferencesManager.setLanguage(it as String) }
            backupData.preferences["syncInterval"]?.let { 
                // Gson might parse numbers as Double, ensuring Long
                val interval = (it as? Number)?.toLong() ?: 1L
                preferencesManager.setSyncInterval(interval) 
            }

            // Restore Database Data
            // 1. Folders first (parent of sources)
            backupData.folders.forEach { folder ->
                feedDao.insertFolder(folder)
            }
            
            // 2. Sources
            backupData.sources.forEach { source ->
                feedDao.insertSource(source)
            }

            // 3. Saved Articles
            // We use insertArticles which is IGNORE conflict strategy usually
            if (backupData.savedArticles.isNotEmpty()) {
                feedDao.insertArticles(backupData.savedArticles)
            }
        }
    }
}
