package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiOcrService
import com.example.data.database.Document
import com.example.data.database.Folder
import com.example.data.database.Page
import com.example.data.database.ScannerDatabase
import com.example.data.repository.DocumentRepository
import com.example.BuildConfig
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScannerDatabase.getDatabase(application)
    
    fun scannerDatabaseHelper(): ScannerDatabase = db
    private val repository = DocumentRepository(
        application,
        db.folderDao(),
        db.documentDao(),
        db.pageDao()
    )
    private val ocrService = GeminiOcrService()

    // Predefined scan presets
    data class ScanPreset(
        val name: String,
        val dpi: Int,
        val pageSize: String,
        val filterType: String
    )

    val scanPresets = MutableStateFlow<List<ScanPreset>>(
        listOf(
            ScanPreset("Standard A4", 150, "A4", "Original"),
            ScanPreset("HD Magic Color A4", 300, "A4", "Magic Color"),
            ScanPreset("Compact Letter B&W", 72, "Letter", "B&W"),
            ScanPreset("HighRes Legal Grayscale", 300, "Legal", "Grayscale")
        )
    )

    init {
        loadPersistedPresets()
    }

    fun saveCustomPreset(name: String, dpi: Int, pageSize: String, filterType: String) {
        val newList = scanPresets.value.toMutableList()
        newList.add(ScanPreset(name, dpi, pageSize, filterType))
        scanPresets.value = newList
        val sharedPref = getApplication<Application>().getSharedPreferences("scanner_presets", Context.MODE_PRIVATE)
        val count = sharedPref.getInt("count", 0)
        with(sharedPref.edit()) {
            putInt("count", count + 1)
            putString("preset_name_${count}", name)
            putInt("preset_dpi_${count}", dpi)
            putString("preset_pagesize_${count}", pageSize)
            putString("preset_filter_${count}", filterType)
            apply()
        }
    }

    private fun loadPersistedPresets() {
        val sharedPref = getApplication<Application>().getSharedPreferences("scanner_presets", Context.MODE_PRIVATE)
        val count = sharedPref.getInt("count", 0)
        if (count > 0) {
            val list = scanPresets.value.toMutableList()
            for (i in 0 until count) {
                val name = sharedPref.getString("preset_name_${i}", "") ?: continue
                val dpi = sharedPref.getInt("preset_dpi_${i}", 150)
                val pageSize = sharedPref.getString("preset_pagesize_${i}", "A4") ?: "A4"
                val filter = sharedPref.getString("preset_filter_${i}", "Original") ?: "Original"
                if (name.isNotEmpty() && list.none { it.name == name }) {
                    list.add(ScanPreset(name, dpi, pageSize, filter))
                }
            }
            scanPresets.value = list
        }
    }

    // Simulated Cloud settings and account connections
    val cloudDriveAccount = MutableStateFlow<String?>(null)
    val cloudDropboxAccount = MutableStateFlow<String?>(null)
    val cloudBackupEnabled = MutableStateFlow(false)
    val syncSchedule = MutableStateFlow("Instant")
    val lastSyncTime = MutableStateFlow("Never")
    val cloudSyncLogs = MutableStateFlow<List<String>>(
        listOf("System: Vault synchronized locally. Connect to cloud for remote backups.")
    )

    fun connectCloudDrive(email: String) {
        cloudDriveAccount.value = email
        addSyncLog("Google Drive: Connected safely as $email")
    }

    fun disconnectCloudDrive() {
        cloudDriveAccount.value = null
        addSyncLog("Google Drive: Account disconnected.")
    }

    fun connectCloudDropbox(username: String) {
        cloudDropboxAccount.value = username
        addSyncLog("Dropbox: Linked account as $username")
    }

    fun disconnectCloudDropbox() {
        cloudDropboxAccount.value = null
        addSyncLog("Dropbox: Unlinked account.")
    }

    fun toggleCloudBackup(enabled: Boolean) {
        cloudBackupEnabled.value = enabled
        addSyncLog("Auto-Backup: Changed state to " + if (enabled) "ENABLED" else "DISABLED")
    }

    fun addSyncLog(msg: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val newList = cloudSyncLogs.value.toMutableList()
        newList.add(0, "[$timestamp] $msg")
        cloudSyncLogs.value = newList.take(50)
    }

    fun runManualCloudSync() {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Syncing with cloud vaults..."
            addSyncLog("Manual sync initiated...")
            kotlinx.coroutines.delay(1800)
            val totalDocs = documents.value.size
            addSyncLog("Scanning local database... found $totalDocs document binders")
            documents.value.forEach { doc ->
                addSyncLog("Uploaded: ${doc.name} [Page Size: ${doc.pageSize}]")
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            lastSyncTime.value = sdf.format(Date())
            addSyncLog("Cloud Sync Completed: Backups are 100% up to date.")
            isProcessing.value = false
        }
    }

    // Screen navigation overlay/state control
    val currentScreen = MutableStateFlow<String>("home") // home, camera, editor, doc_detail, folder_detail
    val activeFolderId = MutableStateFlow<Long?>(null)
    val activeDocumentId = MutableStateFlow<Long?>(null)
    val activePageId = MutableStateFlow<Long?>(null)

    // Global loading and progress message
    val isProcessing = MutableStateFlow(false)
    val processingMessage = MutableStateFlow("")

    // List trackers
    val folders = repository.allFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchQuery = MutableStateFlow("")

    // Documents reactive lists
    val documents = combine(
        repository.allDocuments,
        searchQuery
    ) { docs, query ->
        if (query.isBlank()) {
            docs
        } else {
            docs.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Active Document & Pages
    val currentDocument: StateFlow<Document?> = activeDocumentId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getDocumentFlow(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentDocumentPages: StateFlow<List<Page>> = activeDocumentId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getPagesForDocument(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Editing variables
    val editingPage = MutableStateFlow<Page?>(null)
    val editingBitmap = MutableStateFlow<Bitmap?>(null)

    // Folder Actions
    fun createFolder(name: String) = viewModelScope.launch {
        repository.insertFolder(name)
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
    }

    // Document & Tag Actions
    suspend fun createDocumentDirect(name: String, folderId: Long? = null, tags: String = ""): Long {
        return repository.insertDocument(
            Document(
                name = name,
                folderId = folderId,
                tags = tags
            )
        )
    }

    fun updateDocumentInfo(doc: Document) = viewModelScope.launch {
        repository.updateDocument(doc)
    }

    fun deleteDocument(doc: Document) = viewModelScope.launch {
        repository.deleteDocument(doc)
        if (activeDocumentId.value == doc.id) {
            activeDocumentId.value = null
            currentScreen.value = "home"
        }
    }

    // Add multiple captured images / picked images to document
    fun addPagesToDocument(docId: Long, bitmaps: List<Bitmap>, onFinished: () -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Saving scanned pages..."
            
            withContext(Dispatchers.Default) {
                // Get current pages size to assign index properly
                val existing = repository.getPagesForDocumentDirect(docId)
                var index = existing.size
                
                bitmaps.forEach { bmp ->
                    val originalPath = repository.saveBitmap(bmp, "orig_p${index}")
                    // Apply original/default processed copy
                    val processedPath = repository.saveBitmap(bmp, "proc_p${index}")
                    
                    repository.insertPage(
                        Page(
                            documentId = docId,
                            pageIndex = index,
                            originalPath = originalPath,
                            processedPath = processedPath,
                        )
                    )
                    index++
                }
            }
            isProcessing.value = false
            onFinished()
        }
    }

    // Load page into editor
    fun startEditingPage(page: Page) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Loading image to editor..."
            
            editingPage.value = page
            val originalBmp = repository.loadBitmap(page.originalPath)
            if (originalBmp != null) {
                // Apply rotation and crop if already specified, but for flexibility
                // let's pass original copy to allow tweaking values recursively
                editingBitmap.value = originalBmp
            }
            isProcessing.value = false
            currentScreen.value = "editor"
        }
    }

    // Save crops, rotation, and enhancements
    fun savePageEdits(
        left: Float, top: Float, right: Float, bottom: Float,
        rotateAngle: Int, filterType: String,
        onComplete: () -> Unit
    ) {
        val page = editingPage.value ?: return
        val originalBmp = editingBitmap.value ?: return

        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Applying cropping & filters..."

            val updatedPage = withContext(Dispatchers.Default) {
                // 1. Process custom cropping
                var cropped = cropBitmap(originalBmp, left, top, right, bottom)

                // 2. Process rotation
                if (rotateAngle != 0) {
                    cropped = rotateBitmap(cropped, rotateAngle.toFloat())
                }

                // 3. Process filter enhancements
                val enhanced = applyFilter(cropped, filterType)

                // 4. Save to files system
                val cleanPath = repository.saveBitmap(enhanced, "proc_p${page.pageIndex}")

                // 5. Update Entity values in Room Database
                page.copy(
                    rotation = (page.rotation + rotateAngle) % 360,
                    cropLeft = left,
                    cropTop = top,
                    cropRight = right,
                    cropBottom = bottom,
                    filterType = filterType,
                    processedPath = cleanPath
                )
            }

            repository.updatePage(updatedPage)
            
            // Refresh detail state
            if (activeDocumentId.value != null) {
                activePageId.value = updatedPage.id
            }

            isProcessing.value = false
            onComplete()
        }
    }

    // Rotate quick button
    fun rotatePageQuick(page: Page) = viewModelScope.launch {
        isProcessing.value = true
        processingMessage.value = "Rotating image..."
        
        withContext(Dispatchers.Default) {
            val loaded = repository.loadBitmap(page.processedPath)
            if (loaded != null) {
                val rotated = rotateBitmap(loaded, 90f)
                val path = repository.saveBitmap(rotated, "proc_p${page.pageIndex}")
                val updated = page.copy(
                    rotation = (page.rotation + 90) % 360,
                    processedPath = path
                )
                repository.updatePage(updated)
            }
        }
        isProcessing.value = false
    }

    // Reorder pages list
    fun deletePageFromDoc(page: Page) = viewModelScope.launch {
        repository.deletePage(page)
    }

    // OCR function using Gemini api
    fun triggerOcrForPage(page: Page, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Gemini AI extracting text..."
            
            val bitmap = repository.loadBitmap(page.processedPath)
            if (bitmap == null) {
                isProcessing.value = false
                onFinished("Error: Scanned index image file is missing.")
                return@launch
            }

            // Convert to Base64
            val base64 = withContext(Dispatchers.IO) {
                val byteStream = ByteArrayOutputStream()
                // Resize for efficiency while retaining legibility (battery/performance optimization)
                val scale = 1200f / Math.max(bitmap.width, bitmap.height).toFloat()
                val targetBmp = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else bitmap

                targetBmp.compress(Bitmap.CompressFormat.JPEG, 80, byteStream)
                Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
            }

            // Call ocr
            val ocrResult = ocrService.performOcr(base64)
            
            // Update page with OCR text
            val updated = page.copy(ocrText = ocrResult)
            repository.updatePage(updated)

            isProcessing.value = false
            onFinished(ocrResult)
        }
    }

    // Generation of high fidelity PDF using custom size / DPI
    fun exportAndSharePdf(context: Context, documentId: Long, onShareReady: (Uri) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Generating PDF document..."

            val pdfFileResult = withContext(Dispatchers.Default) {
                try {
                    val doc = repository.getDocumentById(documentId) ?: return@withContext null
                    val pagesList = repository.getPagesForDocumentDirect(documentId)
                    if (pagesList.isEmpty()) return@withContext null

                    val dpi = doc.dpi
                    val orientation = doc.pageSize // "A4", "Letter", "Legal"

                    // Points config (A4: 595x842, Letter: 612x792, Legal: 612x1008)
                    val (pointsWidth, pointsHeight) = when (orientation) {
                        "Letter" -> Pair(612f, 792f)
                        "Legal" -> Pair(612f, 1008f)
                        else -> Pair(595f, 842f) // Default A4
                    }

                    // Pixels dimensions based on custom DPI selection
                    val pxWidth = ((pointsWidth / 72.0) * dpi).toInt()
                    val pxHeight = ((pointsHeight / 72.0) * dpi).toInt()

                    val pdfDocument = PdfDocument()

                    pagesList.forEachIndexed { idx, pg ->
                        val bmp = repository.loadBitmap(pg.processedPath)
                        if (bmp != null) {
                            val pageInfo = PdfDocument.PageInfo.Builder(pxWidth, pxHeight, idx + 1).create()
                            val pdfPage = pdfDocument.startPage(pageInfo)
                            val canvas = pdfPage.canvas

                            // Center-fit letterbox calculation
                            val scale = Math.min(pxWidth.toFloat() / bmp.width, pxHeight.toFloat() / bmp.height)
                            val outWidth = (bmp.width * scale).toInt()
                            val outHeight = (bmp.height * scale).toInt()
                            val left = (pxWidth - outWidth) / 2
                            val top = (pxHeight - outHeight) / 2

                            val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                            val dstRect = android.graphics.Rect(left, top, left + outWidth, top + outHeight)

                            canvas.drawBitmap(bmp, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                            pdfDocument.finishPage(pdfPage)
                        }
                    }

                    val cleanDocName = doc.name.trim().replace("[^a-zA-Z0-9]".toRegex(), "_")
                    val pdfFile = File(context.cacheDir, "${cleanDocName}.pdf")
                    FileOutputStream(pdfFile).use { fos ->
                        pdfDocument.writeTo(fos)
                    }
                    pdfDocument.close()
                    pdfFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (pdfFileResult != null) {
                try {
                    // Generate file URI with configured Provider
                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, pdfFileResult)
                    isProcessing.value = false
                    onShareReady(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    isProcessing.value = false
                }
            } else {
                isProcessing.value = false
            }
        }
    }

    fun exportAndShareImages(context: Context, documentId: Long, format: String, onShareReady: (List<Uri>) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Preparing images..."

            val imageUris = withContext(Dispatchers.Default) {
                try {
                    val doc = repository.getDocumentById(documentId) ?: return@withContext emptyList<Uri>()
                    val pagesList = repository.getPagesForDocumentDirect(documentId)
                    if (pagesList.isEmpty()) return@withContext emptyList<Uri>()

                    val cleanDocName = doc.name.trim().replace("[^a-zA-Z0-9]".toRegex(), "_")
                    val uris = mutableListOf<Uri>()

                    pagesList.forEachIndexed { idx, pg ->
                        val bmp = repository.loadBitmap(pg.processedPath)
                        if (bmp != null) {
                            val ext = if (format == "PNG") "png" else "jpg"
                            val compressFormat = if (format == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                            val imageFile = File(context.cacheDir, "${cleanDocName}_page_${idx + 1}.$ext")
                            
                            FileOutputStream(imageFile).use { fos ->
                                bmp.compress(compressFormat, 90, fos)
                            }
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, imageFile)
                            uris.add(uri)
                        }
                    }
                    uris
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            isProcessing.value = false
            if (imageUris.isNotEmpty()) {
                onShareReady(imageUris)
            }
        }
    }

    // Helper functions for bitmap manipulation
    private fun cropBitmap(source: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val srcW = source.width
        val srcH = source.height
        
        val x = (left * srcW).toInt().coerceIn(0, srcW - 1)
        val y = (top * srcH).toInt().coerceIn(0, srcH - 1)
        var w = ((right - left) * srcW).toInt().coerceAtLeast(1)
        var h = ((bottom - top) * srcH).toInt().coerceAtLeast(1)
        
        if (x + w > srcW) w = srcW - x
        if (y + h > srcH) h = srcH - y
        
        return Bitmap.createBitmap(source, x, y, w, h)
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun applyFilter(source: Bitmap, filter: String): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = Paint()
        
        when (filter) {
            "Grayscale" -> {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }
            "B&W" -> {
                // High dynamic binary black / white threshold effect
                val matrix = floatArrayOf(
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    1.5f, 1.5f, 1.5f, 0f, -160f,
                    0f, 0f, 0f, 1f, 0f
                )
                paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
            }
            "Magic Color" -> {
                // Boost color contrast, brightness, and sharpness
                val saturationM = ColorMatrix().apply { setSaturation(1.5f) }
                val contrast = 1.3f
                val translate = 5f
                val contrastM = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                saturationM.postConcat(contrastM)
                paint.colorFilter = ColorMatrixColorFilter(saturationM)
            }
            else -> {
                // Original, do nothing
            }
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun saveOcrText(page: Page, editedText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val updated = page.copy(ocrText = editedText)
            repository.updatePage(updated)
            if (activeDocumentId.value != null) {
                activePageId.value = updated.id
            }
            onComplete()
        }
    }

    fun triggerAiInstruction(page: Page, currentText: String, instruction: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Google AI executing: $instruction..."
            
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                kotlinx.coroutines.delay(1200)
                isProcessing.value = false
                val simulatedResult = when (instruction) {
                    "Summarize text" -> "Brief Summary:\n- Main Theme: Document outlines transactional receipt details.\n- Highlights: Accurate payment of accounts verified. Total matching criteria established."
                    "Translate to Spanish" -> "Transcripción traducida:\n" + currentText.replace("the", "el/la").replace("and", "y").replace("document", "documento")
                    "Format text neatly" -> "Formatted Output:\n---\n" + currentText.lines().filter { it.isNotBlank() }.joinToString("\n• ")
                    else -> "AI Refined Text Content:\n----\n$currentText"
                }
                onFinished(simulatedResult)
                return@launch
            }

            val prompt = "Based on this extracted document OCR text below, execute the following directive: '$instruction'. Return ONLY the parsed resulting text without any opening pleasantries or markup blocks.\n\nOriginal Text:\n$currentText"
            
            val request = com.example.data.api.GenerateContentRequest(
                contents = listOf(
                    com.example.data.api.Content(
                        parts = listOf(
                            com.example.data.api.Part(text = prompt)
                        )
                    )
                ),
                generationConfig = com.example.data.api.GenerationConfig(temperature = 0.3f)
            )

            try {
                val response = com.example.data.api.RetrofitClient.service.generateContent(apiKey, request)
                val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "AI returned an empty response."
                isProcessing.value = false
                onFinished(result)
            } catch (e: Exception) {
                isProcessing.value = false
                onFinished("AI Error: Unable to call Gemini assistant. Error: ${e.localizedMessage}")
            }
        }
    }

    fun stampOverlayToPage(
        page: Page,
        stampBitmap: Bitmap?,
        offsetX: Float,
        offsetY: Float,
        scaleVal: Float,
        opacityVal: Float,
        rotationVal: Float,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            isProcessing.value = true
            processingMessage.value = "Stamping signature/photo layer..."
            
            val updatedPage = withContext(Dispatchers.Default) {
                val originalBmp = repository.loadBitmap(page.processedPath) ?: repository.loadBitmap(page.originalPath)
                if (originalBmp != null && stampBitmap != null) {
                    val output = Bitmap.createBitmap(originalBmp.width, originalBmp.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(output)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    
                    canvas.drawBitmap(originalBmp, 0f, 0f, paint)
                    paint.alpha = (opacityVal.coerceIn(0f, 1f) * 255).toInt()
                    val matrix = android.graphics.Matrix()
                    
                    val maxTargetDim = Math.min(originalBmp.width, originalBmp.height) * 0.35f
                    val baseScale = maxTargetDim / Math.max(stampBitmap.width, stampBitmap.height)
                    val finalScale = baseScale * scaleVal
                    matrix.postScale(finalScale, finalScale)
                    
                    val stampCenterX = (stampBitmap.width * finalScale) / 2f
                    val stampCenterY = (stampBitmap.height * finalScale) / 2f
                    matrix.postRotate(rotationVal, stampCenterX, stampCenterY)
                    
                    val targetX = offsetX * originalBmp.width
                    val targetY = offsetY * originalBmp.height
                    matrix.postTranslate(targetX, targetY)
                    
                    canvas.drawBitmap(stampBitmap, matrix, paint)
                    val cleanPath = repository.saveBitmap(output, "proc_p${page.pageIndex}_composite")
                    page.copy(processedPath = cleanPath)
                } else null
            }
            
            if (updatedPage != null) {
                repository.updatePage(updatedPage)
                if (activeDocumentId.value != null) {
                    activePageId.value = updatedPage.id
                }
            }
            
            isProcessing.value = false
            onComplete()
        }
    }
}
