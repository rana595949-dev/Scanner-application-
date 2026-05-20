package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: String = "", // Comma-separated tags (e.g., "work,receipt,invoice")
    val dpi: Int = 150, // Export DPI choice (72, 150, 200, 300)
    val pageSize: String = "A4" // Page size choice (A4, Letter, Legal)
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val originalPath: String, // Storage path of original physical capture
    val processedPath: String, // Storage path of cropped/enhanced capture
    val ocrText: String? = null,
    val rotation: Int = 0, // rotation angle (0, 90, 180, 270)
    val cropLeft: Float = 0.0f,
    val cropTop: Float = 0.0f,
    val cropRight: Float = 1.0f,
    val cropBottom: Float = 1.0f,
    val filterType: String = "Original" // "Original", "Magic Color", "Grayscale", "B&W"
)
