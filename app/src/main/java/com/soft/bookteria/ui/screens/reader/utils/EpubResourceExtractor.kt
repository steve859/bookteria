package com.soft.bookteria.ui.screens.reader.utils

import android.content.Context
import android.util.Log
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.ReadingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Utility class for extracting resources (like images) from EPUB files
 * and caching them for display
 */
class EpubResourceExtractor {
    
    companion object {
        private const val TAG = "EpubResourceExtractor"
        private const val CACHE_DIR_NAME = "epub_resources"
        
        /**
         * Creates a cache directory for a specific book
         */
        suspend fun createCacheDir(context: Context, bookId: Int): File? {
            return withContext(Dispatchers.IO) {
                try {
                    // Create a directory structure like:
                    // app_cache/epub_resources/book_123/
                    val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                    if (!cacheDir.exists()) {
                        cacheDir.mkdir()
                    }
                    
                    val bookDir = File(cacheDir, "book_$bookId")
                    if (!bookDir.exists()) {
                        bookDir.mkdir()
                    }
                    
                    bookDir
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating cache directory", e)
                    null
                }
            }
        }
        
        /**
         * Extract resource files (like images) from the EPUB
         * Returns a map of resource IDs to their local file paths
         */
        suspend fun extractResources(context: Context, epubPath: String, bookId: Int): Map<String, String> {
            return withContext(Dispatchers.IO) {
                val resourceMap = mutableMapOf<String, String>()
                try {
                    // Create cache directory
                    val cacheDir = createCacheDir(context, bookId) ?: return@withContext emptyMap()
                    
                    // Open EPUB as ZIP file
                    val zipFile = ZipFile(epubPath)
                    val entries = zipFile.entries()
                    
                    // Process each entry
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        
                        // Skip directories
                        if (entry.isDirectory) continue
                        
                        // Look for image files
                        if (isImageFile(entry.name)) {
                            val extractedFile = extractEntryToFile(zipFile, entry, cacheDir)
                            if (extractedFile != null) {
                                // Map the relative path in EPUB to the local file path
                                resourceMap[entry.name] = extractedFile.absolutePath
                            }
                        }
                    }
                    
                    // Close the zip file
                    zipFile.close()
                    
                    Log.d(TAG, "Extracted ${resourceMap.size} resources from EPUB")
                    resourceMap
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting resources from EPUB", e)
                    emptyMap()
                }
            }
        }
        
        /**
         * Extract an entry from the ZIP file to the cache directory
         */
        private fun extractEntryToFile(zipFile: ZipFile, entry: ZipEntry, destDir: File): File? {
            // Create a sanitized filename from the entry path
            val fileName = entry.name.substringAfterLast('/')
            val destFile = File(destDir, fileName)
            
            try {
                // Extract the file
                zipFile.getInputStream(entry).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return destFile
            } catch (e: IOException) {
                Log.e(TAG, "Failed to extract ${entry.name}", e)
                return null
            }
        }
        
        /**
         * Check if a file is an image based on its extension
         */
        private fun isImageFile(path: String): Boolean {
            val lowerPath = path.lowercase()
            return lowerPath.endsWith(".jpg") || 
                   lowerPath.endsWith(".jpeg") ||
                   lowerPath.endsWith(".png") ||
                   lowerPath.endsWith(".gif") ||
                   lowerPath.endsWith(".webp") ||
                   lowerPath.endsWith(".svg")
        }
        
        /**
         * Clean up cached resources for a book
         */
        suspend fun clearBookCache(context: Context, bookId: Int): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val cacheDir = File(context.cacheDir, "$CACHE_DIR_NAME/book_$bookId")
                    if (cacheDir.exists()) {
                        cacheDir.deleteRecursively()
                    }
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing book cache", e)
                    false
                }
            }
        }
        
        /**
         * Process HTML content to replace relative image URLs with absolute file URLs
         */
        fun processHtmlWithLocalImages(html: String, resourceMap: Map<String, String>): String {
            if (resourceMap.isEmpty()) return html
            
            var processedHtml = html
            
            // Replace image sources with local file paths
            for ((key, value) in resourceMap) {
                // Simplify the key to just the filename in case paths are different
                val simplifiedKey = key.substringAfterLast('/')
                
                // Look for the image in the HTML using various path formats
                val imgPatterns = listOf(
                    "src=\"$key\"",
                    "src='$key'",
                    "src=\"$simplifiedKey\"",
                    "src='$simplifiedKey'"
                )
                
                for (pattern in imgPatterns) {
                    processedHtml = processedHtml.replace(
                        pattern,
                        "src=\"file://$value\""
                    )
                }
            }
            
            return processedHtml
        }
    }
}
