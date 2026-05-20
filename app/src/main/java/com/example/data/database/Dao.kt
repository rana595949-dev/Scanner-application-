package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getDocumentsInFolder(folderId: Long): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId IS NULL ORDER BY createdAt DESC")
    fun getUnassignedDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentFlow(id: Long): Flow<Document?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesForDocumentDirect(documentId: Long): List<Page>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageById(id: Long): Page?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page): Long

    @Update
    suspend fun updatePage(page: Page)

    @Delete
    suspend fun deletePage(page: Page)
}
