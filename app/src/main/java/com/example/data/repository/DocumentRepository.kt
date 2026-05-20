package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.database.Document
import com.example.data.database.DocumentDao
import com.example.data.database.Folder
import com.example.data.database.FolderDao
import com.example.data.database.Page
import com.example.data.database.PageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val context: Context,
    private val folderDao: FolderDao,
    private val documentDao: DocumentDao,
    private val pageDao: PageDao
) {
    // Folders
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insertFolder(name: String): Long = withContext(Dispatchers.IO) {
        folderDao.insertFolder(Folder(name = name))
    }

    suspend fun deleteFolder(folder: Folder) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folder)
    }

    // Documents
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()
    val unassignedDocuments: Flow<List<Document>> = documentDao.getUnassignedDocuments()

    fun getDocumentsInFolder(folderId: Long): Flow<List<Document>> {
        return documentDao.getDocumentsInFolder(folderId)
    }

    fun getDocumentFlow(id: Long): Flow<Document?> {
        return documentDao.getDocumentFlow(id)
    }

    suspend fun getDocumentById(id: Long): Document? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: Document): Long = withContext(Dispatchers.IO) {
        documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: Document) = withContext(Dispatchers.IO) {
        documentDao.updateDocument(document)
    }

    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        // First delete all pages associated physically to free memory space
        val pages = pageDao.getPagesForDocumentDirect(document.id)
        pages.forEach { deletePageFiles(it) }
        documentDao.deleteDocument(document)
    }

    // Pages
    fun getPagesForDocument(documentId: Long): Flow<List<Page>> {
        return pageDao.getPagesForDocument(documentId)
    }

    suspend fun getPagesForDocumentDirect(documentId: Long): List<Page> = withContext(Dispatchers.IO) {
        pageDao.getPagesForDocumentDirect(documentId)
    }

    suspend fun getPageById(id: Long): Page? = withContext(Dispatchers.IO) {
        pageDao.getPageById(id)
    }

    suspend fun insertPage(page: Page): Long = withContext(Dispatchers.IO) {
        pageDao.insertPage(page)
    }

    suspend fun updatePage(page: Page) = withContext(Dispatchers.IO) {
        pageDao.updatePage(page)
    }

    suspend fun deletePage(page: Page) = withContext(Dispatchers.IO) {
        deletePageFiles(page)
        pageDao.deletePage(page)
    }

    // File writing utilities
    suspend fun saveBitmap(bitmap: Bitmap, prefix: String = "scan"): String = withContext(Dispatchers.IO) {
        val storageDir = File(context.filesDir, "scanned_documents")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val file = File(storageDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    suspend fun loadBitmap(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(filePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deletePageFiles(page: Page) {
        try {
            val originalFile = File(page.originalPath)
            if (originalFile.exists()) {
                originalFile.delete()
            }
            val processedFile = File(page.processedPath)
            if (processedFile.exists()) {
                processedFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
