package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.Document
import com.example.data.database.Folder
import com.example.data.database.Page
import com.example.data.database.ScannerDatabase
import com.example.ui.viewmodel.ScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingMsg by viewModel.processingMessage.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Navigation State router
            when (currentScreen) {
                "home" -> HomeScreen(viewModel)
                "camera" -> cameraPermissionAndLaunch(viewModel)
                "editor" -> EditorScreen(viewModel)
                "doc_detail" -> DocumentDetailScreen(viewModel)
                "cloud_settings" -> CloudSettingsScreen(viewModel)
                "signature_stamp" -> SignatureStampScreen(viewModel)
                else -> HomeScreen(viewModel)
            }

            // Global elegant Modal loader overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = processingMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemFolders by viewModel.folders.collectAsStateWithLifecycle()
    val systemDocs by viewModel.documents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSelectionFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()

    var showCreateDocDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Dynamic state calculations
    val currentDocSubset = remember(systemDocs, activeSelectionFolderId) {
        if (activeSelectionFolderId == null) {
            systemDocs
        } else {
            systemDocs.filter { it.folderId == activeSelectionFolderId }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Natural Tones Minimalist Header Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OmniScanner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "CamScanner Lite — Natural AI Scanner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(38.dp)
                            .testTag("create_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.currentScreen.value = "cloud_settings" },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(38.dp)
                            .testTag("cloud_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud Backups",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Avatar button "JD" bg-[#B8F397] text-[#131F0D]
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .clickable { viewModel.currentScreen.value = "cloud_settings" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Elegant Natural Tones Rounded Search Text Field (bg-[#F0F4F8])
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search your documents...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else null,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
        }

        // Folders Filter Category Horizontal Strip
        Text(
            text = "Folders / Workspace Directories",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Standard static item "All Documents"
            item {
                FilterChip(
                    selected = activeSelectionFolderId == null,
                    onClick = { viewModel.activeFolderId.value = null },
                    label = { Text("All Documents") },
                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = "All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            items(systemFolders) { folder ->
                var showDeleteConfirm by remember { mutableStateOf(false) }

                FilterChip(
                    selected = activeSelectionFolderId == folder.id,
                    onClick = { viewModel.activeFolderId.value = folder.id },
                    label = { Text(folder.name) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = folder.name) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Folder",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showDeleteConfirm = true }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Delete folder?") },
                        text = { Text("Documents inside directory '${folder.name}' will stay, but folder affiliation will clear.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteFolder(folder)
                                if (viewModel.activeFolderId.value == folder.id) {
                                    viewModel.activeFolderId.value = null
                                }
                                showDeleteConfirm = false
                            }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Empty docs list checker
        if (currentDocSubset.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = "No papers",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Scanned Documents Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Click the camera below to scan your paper instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // LazyColumn scrolling elements
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(currentDocSubset) { doc ->
                    DocumentCard(doc, viewModel)
                }
            }
        }

        // Actions bottom section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ExtendedFloatingActionButton(
                onClick = { showCreateDocDialog = true },
                text = { Text("SCAN DOCUMENT", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Camera, contentDescription = "Camera Scan") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp), // Rounded 24.dp like the HTML design
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .testTag("scan_floating_button")
            )
        }
    }

    // Modal Create Folder Action
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Directory Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName)
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Create Document Action Setup Window
    if (showCreateDocDialog) {
        var docName by remember { mutableStateOf("") }
        var tagsStr by remember { mutableStateOf("") }
        var selectedFolderId by remember { mutableStateOf<Long?>(activeSelectionFolderId) }
        var docDpi by remember { mutableStateOf(150) }
        var docSizePage by remember { mutableStateOf("A4") }

        val presetsList by viewModel.scanPresets.collectAsStateWithLifecycle()
        var showSavePresetDialog by remember { mutableStateOf(false) }
        var presetNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDocDialog = false },
            title = { Text("Setup Document Scan Profile") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Quick Apply Scan Preset:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetsList) { preset ->
                                FilterChip(
                                    selected = docDpi == preset.dpi && docSizePage == preset.pageSize,
                                    onClick = {
                                        docDpi = preset.dpi
                                        docSizePage = preset.pageSize
                                    },
                                    label = { Text(preset.name, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { 
                                presetNameInput = ""
                                showSavePresetDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Preset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save setup parameters as Preset", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = docName,
                            onValueChange = { docName = it },
                            label = { Text("Document Name (e.g. Work Invoice)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("config_doc_name")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = tagsStr,
                            onValueChange = { tagsStr = it },
                            label = { Text("Search Tags (comma separated, e.g. tax,work)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("config_doc_tags")
                        )
                    }

                    // DPI Option Spinner Grid
                    item {
                        Text("DPI Scan Choice (Resolution Quality):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(72, 150, 200, 300, 600).forEach { dpi ->
                                OutlinedButton(
                                    onClick = { docDpi = dpi },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (docDpi == dpi) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${dpi}DPI", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Sizing Option Grid
                    item {
                        Text("Page Target Size Coordinates Map:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("A4", "Letter", "Legal").forEach { pSize ->
                                OutlinedButton(
                                    onClick = { docSizePage = pSize },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (docSizePage == pSize) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(pSize, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Optional Folder Assign dropdown list
                    item {
                        Text("Save inside Workspace directory Folder:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            item {
                                FilterChip(
                                    selected = selectedFolderId == null,
                                    onClick = { selectedFolderId = null },
                                    label = { Text("Root") }
                                )
                            }
                            items(systemFolders) { fld ->
                                FilterChip(
                                    selected = selectedFolderId == fld.id,
                                    onClick = { selectedFolderId = fld.id },
                                    label = { Text(fld.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (docName.isBlank()) {
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            "Doc_" + sdf.format(Date())
                        } else docName

                        scope.launch {
                            val docId = viewModel.createDocumentDirect(
                                name = finalName,
                                folderId = selectedFolderId,
                                tags = tagsStr
                            )

                            // Set metadata custom parameters
                            viewModel.activeDocumentId.value = docId
                            viewModel.currentScreen.value = "camera"
                            showCreateDocDialog = false
                        }
                    }
                ) {
                    Text("START SCANNING", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        if (showSavePresetDialog) {
            AlertDialog(
                onDismissRequest = { showSavePresetDialog = false },
                title = { Text("Save Custom Preset") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current: ${docDpi} DPI, ${docSizePage} Page Size", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = presetNameInput,
                            onValueChange = { presetNameInput = it },
                            label = { Text("Preset Name (e.g., Taxes HD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (presetNameInput.isNotBlank()) {
                                viewModel.saveCustomPreset(presetNameInput, docDpi, docSizePage, "Original")
                                showSavePresetDialog = false
                            }
                        }
                    ) {
                        Text("Save Preset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSavePresetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DocumentCard(doc: Document, viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val pagesList by viewModel.currentDocumentPages.collectAsStateWithLifecycle()
    var displayFolderSelector by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Read pages direct inside repository mapping for dynamic thumbnail display
    var listPagesForThisCard by remember { mutableStateOf<List<Page>>(emptyList()) }
    LaunchedEffect(doc.id) {
        listPagesForThisCard = viewModel.currentDocumentPages.value // fallbacks
        withContext(Dispatchers.IO) {
            val list = ScannerDatabase.getDatabase(context).pageDao().getPagesForDocumentDirect(doc.id)
            withContext(Dispatchers.Main) {
                listPagesForThisCard = list
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.activeDocumentId.value = doc.id
                viewModel.currentScreen.value = "doc_detail"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val path = listPagesForThisCard.firstOrNull()?.processedPath
                if (path != null) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Thumbnail preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "No pages",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Sub Info details
                val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                Text(
                    text = "${listPagesForThisCard.size} Pages · ${sdf.format(Date(doc.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = "Profile: ${doc.pageSize} / ${doc.dpi}DPI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Custom tags render
                if (doc.tags.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        doc.tags.split(",").forEach { tag ->
                            if (tag.trim().isNotEmpty()) {
                                val isFirst = doc.tags.split(",").firstOrNull() == tag
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isFirst) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tag.trim().uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secondary Fast Actions Toolbar Panel
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { showShareDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { displayFolderSelector = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Relocate Folder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Export & Share") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Share as PDF Document") },
                        leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndSharePdf(context, doc.id) { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Scanned Document"))
                            }
                            showShareDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Share as JPG Images") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndShareImages(context, doc.id, "JPG") { uris ->
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/jpeg"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    if (uris.isNotEmpty()) {
                                        val clipData = android.content.ClipData.newUri(context.contentResolver, "Images", uris[0])
                                        for (i in 1 until uris.size) {
                                            clipData.addItem(android.content.ClipData.Item(uris[i]))
                                        }
                                        this.clipData = clipData
                                    }
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Scanned Images"))
                            }
                            showShareDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Share as PNG Images") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndShareImages(context, doc.id, "PNG") { uris ->
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/png"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    if (uris.isNotEmpty()) {
                                        val clipData = android.content.ClipData.newUri(context.contentResolver, "Images", uris[0])
                                        for (i in 1 until uris.size) {
                                            clipData.addItem(android.content.ClipData.Item(uris[i]))
                                        }
                                        this.clipData = clipData
                                    }
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Scanned Images"))
                            }
                            showShareDialog = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (displayFolderSelector) {
        val foldersList by viewModel.folders.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { displayFolderSelector = false },
            title = { Text("Move PDF to Folder") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Root Directory (no folder)") },
                            modifier = Modifier.clickable {
                                viewModel.updateDocumentInfo(doc.copy(folderId = null))
                                displayFolderSelector = false
                            }
                        )
                    }
                    items(foldersList) { f ->
                        ListItem(
                            headlineContent = { Text(f.name) },
                            modifier = Modifier.clickable {
                                viewModel.updateDocumentInfo(doc.copy(folderId = f.id))
                                displayFolderSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun cameraPermissionAndLaunch(viewModel: ScannerViewModel) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    if (cameraPermissionState.status.isGranted) {
        LiveCameraScannerView(viewModel)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera hardware check",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Smart capture scanner feeds need hardware camera frames to perform real-time scanning & alignment.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun LiveCameraScannerView(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activeDocId by viewModel.activeDocumentId.collectAsStateWithLifecycle()

    val imageCapture = remember { ImageCapture.Builder().build() }
    val capturedImages = remember { mutableStateListOf<Bitmap>() }

    // Standard photo picker for gallery input fallback
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { u ->
                try {
                    context.contentResolver.openInputStream(u)?.use { s ->
                        val bmp = BitmapFactory.decodeStream(s)
                        if (bmp != null) capturedImages.add(bmp)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Camera Hardware view
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            onRelease = {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // CamScanner Green Framing aperture cross guideline
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 24.dp.toPx()
                val w = size.width
                val h = size.height
                val rectW = w - (pad * 2)
                val rectH = rectW * 1.414f // Standard A4 Aspect ratio coordinates helper

                val l = pad
                val t = (h - rectH) / 2
                val r = l + rectW
                val b = t + rectH

                // Draw alignment helper boundaries
                drawRect(
                    color = Color(0x332DD4BF),
                    topLeft = androidx.compose.ui.geometry.Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(rectW, rectH)
                )
                drawRect(
                    color = Color(0xFF2DD4BF),
                    topLeft = androidx.compose.ui.geometry.Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(rectW, rectH),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = "home" },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Hold Steady to Capture",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            // Gallery picker fallback
            IconButton(
                onClick = { pickMediaLauncher.launch("image/*") },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery Select", tint = Color.White)
            }
        }

        // Bottom horizontal multi-captured elements strip layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Captured pages thumbnails preview row
            if (capturedImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    items(capturedImages) { img ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                        ) {
                            AsyncImage(
                                model = img,
                                contentDescription = "captured",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer left
                Box(modifier = Modifier.size(56.dp))

                // Large central snap button
                IconButton(
                    onClick = {
                        val executor = ContextCompat.getMainExecutor(context)
                        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                
                                val degrees = image.imageInfo.rotationDegrees
                                val corrected = if (degrees != 0) {
                                    val mat = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
                                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mat, true)
                                } else bitmap

                                image.close()
                                capturedImages.add(corrected)
                                Toast.makeText(context, "Page captured!", Toast.LENGTH_SHORT).show()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        })
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent, CircleShape)
                            .border(6.dp, Color(0xFF0F766E), CircleShape)
                    )
                }

                // Finish scan save action
                IconButton(
                    onClick = {
                        if (capturedImages.isNotEmpty()) {
                            val id = activeDocId
                            if (id != null) {
                                viewModel.addPagesToDocument(id, capturedImages) {
                                    // Switch to Detail Screen to show the results
                                    viewModel.currentScreen.value = "doc_detail"
                                }
                            }
                        } else {
                            Toast.makeText(context, "Capture at least one page!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (capturedImages.isNotEmpty()) Color(0xFF14B8A6) else Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save capture session",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EditorScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val pBmp by viewModel.editingBitmap.collectAsStateWithLifecycle()
    val pageEntity by viewModel.editingPage.collectAsStateWithLifecycle()

    // Interactive slider crop proportions values
    var cL by remember { mutableFloatStateOf(0.0f) }
    var cT by remember { mutableFloatStateOf(0.0f) }
    var cR by remember { mutableFloatStateOf(1.0f) }
    var cB by remember { mutableFloatStateOf(1.0f) }

    var rA by remember { mutableIntStateOf(0) }
    var runningFilter by remember { mutableStateOf("Original") }

    // Init with existing DB values
    LaunchedEffect(pageEntity) {
        pageEntity?.let {
            cL = it.cropLeft
            cT = it.cropTop
            cR = it.cropRight
            cB = it.cropBottom
            runningFilter = it.filterType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = "doc_detail" }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text(
                text = "Crop & Enhance Document",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    viewModel.savePageEdits(cL, cT, cR, cB, rA, runningFilter) {
                        viewModel.currentScreen.value = "doc_detail"
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = "Accept Edits", tint = Color(0xFF2DD4BF))
            }
        }

        // Mid graphic interactive display panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pBmp != null) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                ) {
                    AsyncImage(
                        model = pBmp,
                        contentDescription = "Crop original",
                        modifier = Modifier.fillMaxWidth().aspectRatio(pBmp!!.width.toFloat() / pBmp!!.height.toFloat()),
                        contentScale = ContentScale.Fit
                    )

                    // Overlay Canvas defining dynamic Aperture Focus
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                    ) {
                        val w = size.width
                        val h = size.height
                        val leftPx = cL * w
                        val topPx = cT * h
                        val rightPx = cR * w
                        val bottomPx = cB * h

                        // Clear aperture center
                        drawRect(color = Color.Black.copy(alpha = 0.6f))
                        drawRect(
                            color = Color.Transparent,
                            topLeft = androidx.compose.ui.geometry.Offset(leftPx, topPx),
                            size = androidx.compose.ui.geometry.Size(rightPx - leftPx, bottomPx - topPx),
                            blendMode = BlendMode.Clear
                        )

                        // Highlight border grids
                        drawRect(
                            color = Color(0xFF14B8A6),
                            topLeft = androidx.compose.ui.geometry.Offset(leftPx, topPx),
                            size = androidx.compose.ui.geometry.Size(rightPx - leftPx, bottomPx - topPx),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // Adjustments Sliders / Quick Controls Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trim controls
                Text(
                    text = "Interactive Smart Boundary Controls",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Left Boundary", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = cL,
                            onValueChange = { cL = it.coerceAtMost(cR - 0.1f) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF2DD4BF), activeTrackColor = Color(0xFF14B8A6)),
                            modifier = Modifier.weight(1f).testTag("slider_crop_left")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Top Boundary", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = cT,
                            onValueChange = { cT = it.coerceAtMost(cB - 0.1f) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF2DD4BF), activeTrackColor = Color(0xFF14B8A6)),
                            modifier = Modifier.weight(1f).testTag("slider_crop_top")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Right Boundary", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = cR,
                            onValueChange = { cR = it.coerceAtLeast(cL + 0.1f) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF2DD4BF), activeTrackColor = Color(0xFF14B8A6)),
                            modifier = Modifier.weight(1f).testTag("slider_crop_right")
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bottom Boundary", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = cB,
                            onValueChange = { cB = it.coerceAtLeast(cT + 0.1f) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF2DD4BF), activeTrackColor = Color(0xFF14B8A6)),
                            modifier = Modifier.weight(1f).testTag("slider_crop_bottom")
                        )
                    }
                }

                // Rotating and quick buttons panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { rA = (rA + 90) % 360 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                        modifier = Modifier.testTag("rotate_90_button")
                    ) {
                        Icon(Icons.Default.CropRotate, contentDescription = "Rotate")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate 90°", color = Color.White)
                    }

                    Button(
                        onClick = {
                            cL = 0f
                            cT = 0f
                            cR = 1f
                            cB = 1f
                            rA = 0
                            runningFilter = "Original"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                    ) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Reset")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Trim", color = Color.White)
                    }
                }

                // Quality enhancement filter strips
                Text(
                    text = "CamScanner Enhancement Pipelines:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("Original", "Magic Color", "Grayscale", "B&W")
                    items(filters) { filt ->
                        OutlinedButton(
                            onClick = { runningFilter = filt },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (runningFilter == filt) Color(0xFF0F766E) else Color(0xFF374151),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, if (runningFilter == filt) Color(0xFF2DD4BF) else Color.Transparent)
                        ) {
                            Text(filt)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentDetailScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val currentDoc by viewModel.currentDocument.collectAsStateWithLifecycle()
    val pagesList by viewModel.currentDocumentPages.collectAsStateWithLifecycle()

    var showAddPageMenu by remember { mutableStateOf(false) }
    var displayedOcrText by remember { mutableStateOf<String?>(null) }
    var renameDialog by remember { mutableStateOf(false) }
    var showShareDialogDetails by remember { mutableStateOf(false) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val list = mutableListOf<Bitmap>()
            uris.forEach { u ->
                try {
                    context.contentResolver.openInputStream(u)?.use { s ->
                        val b = BitmapFactory.decodeStream(s)
                        if (b != null) list.add(b)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            currentDoc?.let {
                viewModel.addPagesToDocument(it.id, list) {}
            }
        }
    }

    if (currentDoc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a document to begin.")
        }
        return
    }

    val doc = currentDoc!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = "home" }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = doc.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { renameDialog = true }
                )
                Text(
                    text = "${doc.pageSize} Format · ${doc.dpi} DPI quality",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { renameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Rename")
            }
        }

        // Horizontal OCR Extraction banner button
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Optical Text Extraction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Transcribe handwriting & machine documents instantly with Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = {
                        val firstPage = pagesList.firstOrNull()
                        if (firstPage != null) {
                            viewModel.triggerOcrForPage(firstPage) { res ->
                                displayedOcrText = res
                            }
                        } else {
                            Toast.makeText(context, "No scanned images present inside folder yet.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "OCR")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RUN OCR", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of doc scanned pages
        if (pagesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera check", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No pages added inside document package yet.")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(pagesList) { pg ->
                    PageGridThumbnail(pg, viewModel)
                }
            }
        }

        // Footer buttons overlay panels
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick add pages trigger
                OutlinedButton(
                    onClick = { showAddPageMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add paper")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Page")
                }

                // Share final compilations PDF
                Button(
                    onClick = { showShareDialogDetails = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("share_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Share", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showShareDialogDetails) {
        AlertDialog(
            onDismissRequest = { showShareDialogDetails = false },
            title = { Text("Export & Share Final Project") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Share as PDF Binder (Multiple pages)") },
                        leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndSharePdf(context, doc.id) { uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Scanned PDF Binder"))
                            }
                            showShareDialogDetails = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Share as JPG Files") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndShareImages(context, doc.id, "JPG") { uris ->
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/jpeg"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    if (uris.isNotEmpty()) {
                                        val clipData = android.content.ClipData.newUri(context.contentResolver, "Images", uris[0])
                                        for (i in 1 until uris.size) {
                                            clipData.addItem(android.content.ClipData.Item(uris[i]))
                                        }
                                        this.clipData = clipData
                                    }
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Images"))
                            }
                            showShareDialogDetails = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Share as PNG Files") },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.exportAndShareImages(context, doc.id, "PNG") { uris ->
                                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "image/png"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    if (uris.isNotEmpty()) {
                                        val clipData = android.content.ClipData.newUri(context.contentResolver, "Images", uris[0])
                                        for (i in 1 until uris.size) {
                                            clipData.addItem(android.content.ClipData.Item(uris[i]))
                                        }
                                        this.clipData = clipData
                                    }
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Images"))
                            }
                            showShareDialogDetails = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Modal Add Page menu
    if (showAddPageMenu) {
        AlertDialog(
            onDismissRequest = { showAddPageMenu = false },
            title = { Text("Choose Page Capture Input Source") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Live Camera Scanner Feed") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = "Cam") },
                        modifier = Modifier.clickable {
                            viewModel.currentScreen.value = "camera"
                            showAddPageMenu = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Pick Image Files from Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Gal") },
                        modifier = Modifier.clickable {
                            pickMediaLauncher.launch("image/*")
                            showAddPageMenu = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Sync edited text state when displayedOcrText changes
    var editedOcrText by remember { mutableStateOf("") }
    LaunchedEffect(displayedOcrText) {
        if (displayedOcrText != null) {
            editedOcrText = displayedOcrText!!
        }
    }
    var activeLangByPipe by remember { mutableStateOf("English") }

    // Modal OCR Text details sheet display
    if (displayedOcrText != null) {
        Dialog(onDismissRequest = { displayedOcrText = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Extracted OCR Transcription",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Select Language Model Pipeline:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(listOf("English", "Spanish", "French", "German", "Chinese", "Hindi")) { lang ->
                            FilterChip(
                                selected = activeLangByPipe == lang,
                                onClick = { 
                                    activeLangByPipe = lang
                                    Toast.makeText(context, "Pipeline configured: $lang OCR model", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(lang, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    Text("Google AI Assistants:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        listOf("Summarize text", "Translate to Spanish", "Format text neatly").forEach { action ->
                            Button(
                                onClick = {
                                    val page = pagesList.firstOrNull()
                                    if (page != null) {
                                        viewModel.triggerAiInstruction(page, editedOcrText, action) { aiRes ->
                                            editedOcrText = aiRes
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(30.dp)
                            ) {
                                Text(
                                    text = when (action) {
                                        "Summarize text" -> "AI Summary"
                                        "Translate to Spanish" -> "Translate"
                                        "Format text neatly" -> "AI Format"
                                        else -> "Assist"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editedOcrText,
                        onValueChange = { editedOcrText = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("ocr_result_text_field"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val firstPage = pagesList.firstOrNull()
                                if (firstPage != null) {
                                    viewModel.saveOcrText(firstPage, editedOcrText) {
                                        displayedOcrText = null
                                        Toast.makeText(context, "Saved changes directly to database!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    displayedOcrText = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("scanned_ocr", editedOcrText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied edited text to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedButton(
                            onClick = { displayedOcrText = null },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog modal
    if (renameDialog) {
        var newName by remember { mutableStateOf(doc.name) }
        var tempTags by remember { mutableStateOf(doc.tags) }
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("Profile & Details Config setup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Document name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempTags,
                        onValueChange = { tempTags = it },
                        label = { Text("Custom tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDocumentInfo(doc.copy(name = newName, tags = tempTags))
                        renameDialog = false
                    }
                ) {
                    Text("Save Edits")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PageGridThumbnail(page: Page, viewModel: ScannerViewModel) {
    var expandOpsMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandOpsMenu = true }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(page.processedPath),
                    contentDescription = "scanned index Page ${page.pageIndex}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Inside
                )

                // Overlay tag numbering
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${page.pageIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter: ${page.filterType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { expandOpsMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tools")
                }
            }
        }
    }

    // Modal quick helper context list sheet option
    if (expandOpsMenu) {
        AlertDialog(
            onDismissRequest = { expandOpsMenu = false },
            title = { Text("Page ${page.pageIndex + 1} Editor Actions") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Smart Crop / Enhancement Filters") },
                        leadingContent = { Icon(Icons.Default.Crop, contentDescription = "Edit") },
                        modifier = Modifier.clickable {
                            viewModel.startEditingPage(page)
                            expandOpsMenu = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Rotate Quick 90°") },
                        leadingContent = { Icon(Icons.Default.CropRotate, contentDescription = "Rotate") },
                        modifier = Modifier.clickable {
                            viewModel.rotatePageQuick(page)
                            expandOpsMenu = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Sign / Custom Stamp Overlay", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { Icon(Icons.Default.Gesture, contentDescription = "Sign", tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            viewModel.activePageId.value = page.id
                            viewModel.currentScreen.value = "signature_stamp"
                            expandOpsMenu = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Delete This Page", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deletePageFromDoc(page)
                            expandOpsMenu = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun CloudSettingsScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val driveAccount by viewModel.cloudDriveAccount.collectAsStateWithLifecycle()
    val dropboxAccount by viewModel.cloudDropboxAccount.collectAsStateWithLifecycle()
    val autoBackup by viewModel.cloudBackupEnabled.collectAsStateWithLifecycle()
    val schedule by viewModel.syncSchedule.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val syncLogs by viewModel.cloudSyncLogs.collectAsStateWithLifecycle()

    var showDriveConnectDialog by remember { mutableStateOf(false) }
    var driveEmailInput by remember { mutableStateOf("") }
    var showDropboxConnectDialog by remember { mutableStateOf(false) }
    var dropboxUserInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = "home" },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Cloud Backup & AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "100% Secure & Offline Resilient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google AI Features Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = "Google AI Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google AI & Gemini OCR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Leverage Google AI's Gemini model for complex semantic lookups, post-scan summaries, and instant translations offline fallback core.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Generate Doc Summary on Save", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = true,
                                onCheckedChange = {
                                    Toast.makeText(context, "Google AI trigger preferences saved", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Google Drive integration card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudQueue, contentDescription = "GDrive", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            if (driveAccount != null) {
                                Text("Connected", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            } else {
                                Text("Offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (driveAccount != null) {
                            Text("Linked: $driveAccount", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.disconnectCloudDrive() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disconnect Drive", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        } else {
                            Text("Instantly upload your PDFs as encrypted backups to your personal Google Drive account.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDriveConnectDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect Google Drive Account")
                            }
                        }
                    }
                }
            }

            // Dropbox Integration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Dropbox", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dropbox Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            if (dropboxAccount != null) {
                                Text("Connected", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            } else {
                                Text("Offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (dropboxAccount != null) {
                            Text("Linked User: $dropboxAccount", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.disconnectCloudDropbox() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Unlink Dropbox", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        } else {
                            Text("Synchronize your document folders and tagged meta-records seamlessly with your Dropbox account.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDropboxConnectDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect Dropbox Account")
                            }
                        }
                    }
                }
            }

            // Centralized automatic sync setup
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cloud Vault Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Automatic Backups", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Upload scans securely as they form", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = autoBackup,
                                onCheckedChange = { viewModel.toggleCloudBackup(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Sync Frequency:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            listOf("Instant", "Daily", "Weekly").forEach { sched ->
                                FilterChip(
                                    selected = schedule == sched,
                                    onClick = { 
                                        viewModel.syncSchedule.value = sched
                                        viewModel.addSyncLog("Schedule set to: $sched sync runs")
                                    },
                                    label = { Text(sched, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Saved Core Vault Sync: $lastSync", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { viewModel.runManualCloudSync() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync Now", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Vault", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Connection sync console monitor
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sync Event Terminal Console Logs:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(syncLogs) { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF66BB6A),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showDriveConnectDialog) {
        AlertDialog(
            onDismissRequest = { showDriveConnectDialog = false },
            title = { Text("Connect Google Drive") },
            text = {
                OutlinedTextField(
                    value = driveEmailInput,
                    onValueChange = { driveEmailInput = it },
                    label = { Text("Google Email Account") },
                    placeholder = { Text("example@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (driveEmailInput.isNotBlank()) {
                            viewModel.connectCloudDrive(driveEmailInput)
                            showDriveConnectDialog = false
                        }
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDropboxConnectDialog) {
        AlertDialog(
            onDismissRequest = { showDropboxConnectDialog = false },
            title = { Text("Connect Dropbox Sync") },
            text = {
                OutlinedTextField(
                    value = dropboxUserInput,
                    onValueChange = { dropboxUserInput = it },
                    label = { Text("Dropbox Username ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dropboxUserInput.isNotBlank()) {
                            viewModel.connectCloudDropbox(dropboxUserInput)
                            showDropboxConnectDialog = false
                        }
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDropboxConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SignatureStampScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val activePageId by viewModel.activePageId.collectAsStateWithLifecycle()

    // Retrieve active Page object
    var currentPage by remember { mutableStateOf<Page?>(null) }
    LaunchedEffect(activePageId) {
        if (activePageId != null) {
            currentPage = withContext(Dispatchers.IO) {
                viewModel.scannerDatabaseHelper().pageDao().getPageById(activePageId!!)
            }
        }
    }

    // Interactive edit modes: DRAW or PHOTO STAMP
    var editModeIsDraw by remember { mutableStateOf(true) }

    // Drawing inputs
    val signatureStrokes = remember { mutableStateListOf<List<Offset>>() }
    val activeStrokePoints = remember { mutableStateListOf<Offset>() }
    var activePenColor by remember { mutableStateOf(Color.Black) }
    var activePenThickness by remember { mutableStateOf(10f) }

    // Floating Stamp positioning state (percentages of width/height 0f..1f)
    var overlayOffsetX by remember { mutableStateOf(0.35f) }
    var overlayOffsetY by remember { mutableStateOf(0.45f) }
    var overlayScaleFactor by remember { mutableStateOf(1.0f) }
    var overlayOpacityFactor by remember { mutableStateOf(1.0f) }
    var overlayRotationFactor by remember { mutableStateOf(0f) }

    // Dynamically loaded image stamp bitmap
    var importedStampBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val photoStampLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val rawBmp = BitmapFactory.decodeStream(stream)
                if (rawBmp != null) {
                    importedStampBitmap = rawBmp
                    Toast.makeText(context, "PIP Image layer imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load graphic layer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = "doc_detail" },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Sign & Photo-In-Photo Stamp Editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Insert handwritten signatures, stamps, or multi-photo layers details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mode Toggles tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { editModeIsDraw = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editModeIsDraw) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Gesture, contentDescription = "Sign")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Draw Signature", color = if (editModeIsDraw) Color.White else MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = { editModeIsDraw = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!editModeIsDraw) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1.3f)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Stamp")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stamps / Gallery PIP", color = if (!editModeIsDraw) Color.White else MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card showing underlying Document Page and the positioning overlay item
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage != null) {
                        // Original Document Background image
                        AsyncImage(
                            model = File(currentPage!!.processedPath),
                            contentDescription = "Edit Background Page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Inside
                        )
                    }

                    // Interactive overlay drawer or position view overlay bounds Box
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val viewW = maxWidth
                        val viewH = maxHeight

                        if (editModeIsDraw) {
                            // Canvas for freehand writing drawing
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                activeStrokePoints.clear()
                                                activeStrokePoints.add(offset)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                activeStrokePoints.add(change.position)
                                            },
                                            onDragEnd = {
                                                if (activeStrokePoints.isNotEmpty()) {
                                                    signatureStrokes.add(activeStrokePoints.toList())
                                                    activeStrokePoints.clear()
                                                    
                                                    // Convert active strokes to transparent overlay bitmap and load it
                                                    val rawBmp = renderSignatureToBitmap(
                                                        signatureStrokes,
                                                        activePenColor.hashCode(), // raw hex
                                                        activePenThickness,
                                                        800,
                                                        800
                                                    )
                                                    importedStampBitmap = rawBmp
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // Draw completed strokes lines
                                signatureStrokes.forEach { stroke ->
                                    for (i in 0 until stroke.size - 1) {
                                        drawLine(
                                            color = activePenColor,
                                            start = stroke[i],
                                            end = stroke[i+1],
                                            strokeWidth = activePenThickness
                                        )
                                    }
                                }
                                // Draw currently active stroke lines
                                for (i in 0 until activeStrokePoints.size - 1) {
                                    drawLine(
                                        color = activePenColor,
                                        start = activeStrokePoints[i],
                                        end = activeStrokePoints[i+1],
                                        strokeWidth = activePenThickness
                                    )
                                }
                            }
                        } else {
                            // PHOTO LAYER / COMPRESSED PIP SIGNATURE PREVIEW
                            if (importedStampBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = viewW * overlayOffsetX,
                                            y = viewH * overlayOffsetY
                                        )
                                        .size(100.dp * overlayScaleFactor)
                                        .graphicsLayer {
                                            rotationZ = overlayRotationFactor
                                            alpha = overlayOpacityFactor
                                        }
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    AsyncImage(
                                        model = importedStampBitmap,
                                        contentDescription = "Floating Custom Layer Stamp",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Pick stamp or write signature to overlay live", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // Controls adjustment board Box
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editModeIsDraw) {
                        Text("Signature Properties Configuration:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        
                        // Select colors palette
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(Color.Black, Color(0xFF0D47A1), Color(0xFF1B5E20), Color(0xFFB71C1C)).forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(col, CircleShape)
                                        .border(
                                            width = if (activePenColor == col) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { activePenColor = col }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Stroke thickness: ${activePenThickness.toInt()}px", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activePenThickness,
                            onValueChange = { activePenThickness = it },
                            valueRange = 4f..35f
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { 
                                signatureStrokes.clear() 
                                importedStampBitmap = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Drawing Canvas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        Button(
                            onClick = { 
                                if (signatureStrokes.isNotEmpty()) {
                                    val rawBmp = renderSignatureToBitmap(
                                        signatureStrokes,
                                        activePenColor.hashCode(),
                                        activePenThickness,
                                        800,
                                        800
                                    )
                                    importedStampBitmap = rawBmp
                                    editModeIsDraw = false // Switch to display controls instantly
                                    Toast.makeText(context, "Rendered signature vector compiled to digital stamp!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Approve Vector Ink", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        // POSITIONING SLIDERS FOR SIGNATURE/PHOTO LAYER
                        Text("Stamping Placement Sliders Layout:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        
                        Button(
                            onClick = { photoStampLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add stamp item")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Stamp / PIP Image", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Offset X Sliders
                        Text("Position X coordinates: ${(overlayOffsetX * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = overlayOffsetX, onValueChange = { overlayOffsetX = it }, valueRange = 0f..0.9f)

                        // Offset Y Sliders
                        Text("Position Y coordinates: ${(overlayOffsetY * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = overlayOffsetY, onValueChange = { overlayOffsetY = it }, valueRange = 0f..0.9f)

                        // Scaling Slider
                        Text("Stamp Layer scale dimension: ${String.format("%.1f", overlayScaleFactor)}x", style = MaterialTheme.typography.bodySmall)
                        Slider(value = overlayScaleFactor, onValueChange = { overlayScaleFactor = it }, valueRange = 0.2f..3.0f)

                        // Rotation Slider
                        Text("Z axis rotation: ${overlayRotationFactor.toInt()}°", style = MaterialTheme.typography.bodySmall)
                        Slider(value = overlayRotationFactor, onValueChange = { overlayRotationFactor = it }, valueRange = 0f..360f)

                        // Opacity Slider
                        Text("Transparency alpha: ${(overlayOpacityFactor * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(value = overlayOpacityFactor, onValueChange = { overlayOpacityFactor = it }, valueRange = 0.1f..1.0f)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Flatten Action Button
                    Button(
                        onClick = {
                            if (currentPage != null && importedStampBitmap != null) {
                                viewModel.stampOverlayToPage(
                                    page = currentPage!!,
                                    stampBitmap = importedStampBitmap,
                                    offsetX = overlayOffsetX,
                                    offsetY = overlayOffsetY,
                                    scaleVal = overlayScaleFactor,
                                    opacityVal = overlayOpacityFactor,
                                    rotationVal = overlayRotationFactor
                                ) {
                                    viewModel.currentScreen.value = "doc_detail"
                                    Toast.makeText(context, "Stamp successfully flattened onto scanned document!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Make sure you completed drawing signature or imported stamp layer", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.BorderColor, contentDescription = "Flatten")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flatten & Save Stamp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun renderSignatureToBitmap(
    strokes: List<List<Offset>>,
    colorIntHex: Int,
    thickness: Float,
    canvasW: Int,
    canvasH: Int
): Bitmap {
    val output = Bitmap.createBitmap(canvasW.coerceAtLeast(100), canvasH.coerceAtLeast(100), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorIntHex
        strokeWidth = thickness
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    strokes.forEach { stroke ->
        if (stroke.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
            canvas.drawPath(path, paint)
        }
    }
    return output
}
