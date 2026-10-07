package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PdfRepository
import com.example.engine.PdfExporter
import com.example.engine.PdfRendererHelper
import com.example.model.BookmarkItem
import com.example.model.DrawingAnnotation
import com.example.model.DrawingPoint
import com.example.model.EditorTool
import com.example.model.ImageAnnotation
import com.example.model.PdfDocumentItem
import com.example.model.ReaderTheme
import com.example.model.ShapeAnnotation
import com.example.model.ShapeType
import com.example.model.SignatureAnnotation
import com.example.model.StampType
import com.example.model.TextAnnotation
import com.example.model.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PdfUiState(
    val documents: List<PdfDocumentItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: String = "ALL", // ALL, STARRED, SAMPLES
    val isLoadingLibrary: Boolean = false,

    // Active Document
    val activeDoc: PdfDocumentItem? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val readerTheme: ReaderTheme = ReaderTheme.LIGHT,
    val viewMode: ViewMode = ViewMode.SINGLE,
    val activeTool: EditorTool = EditorTool.READ,
    val zoomScale: Float = 1.0f,

    // Annotations for active document
    val textAnnotations: List<TextAnnotation> = emptyList(),
    val imageAnnotations: List<ImageAnnotation> = emptyList(),
    val drawingAnnotations: List<DrawingAnnotation> = emptyList(),
    val shapeAnnotations: List<ShapeAnnotation> = emptyList(),
    val signatureAnnotations: List<SignatureAnnotation> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),

    // Page customisations
    val pageRotations: Map<Int, Int> = emptyMap(),
    val deletedPages: Set<Int> = emptySet(),

    // Active drawing tool settings
    val drawStrokeColor: Long = 0xFFDC2626,
    val drawStrokeWidth: Float = 4f,
    val isHighlighter: Boolean = false,

    // Selected item for manipulation
    val selectedTextId: String? = null,
    val selectedImageId: String? = null,

    // Export & dialogs
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val snackbarMessage: String? = null
)

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application)
    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    private var activeRendererHelper: PdfRendererHelper? = null

    // Undo stack for annotations
    private val undoStack = mutableListOf<PdfUiState>()
    private val redoStack = mutableListOf<PdfUiState>()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLibrary = true) }
            val docs = repository.getDocuments()
            _uiState.update { it.copy(documents = docs, isLoadingLibrary = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setActiveFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun importPdfFromUri(uri: Uri, fileName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLibrary = true) }
            val item = repository.importPdfFromUri(uri, fileName)
            if (item != null) {
                loadDocuments()
                openDocument(item)
            } else {
                _uiState.update { it.copy(isLoadingLibrary = false, snackbarMessage = "Failed to import PDF file") }
            }
        }
    }

    fun createBlankDocument(title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLibrary = true) }
            val item = repository.createBlankDocument(title)
            loadDocuments()
            openDocument(item)
        }
    }

    fun toggleFavorite(doc: PdfDocumentItem) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id)
            loadDocuments()
        }
    }

    fun deleteDocument(doc: PdfDocumentItem) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (_uiState.value.activeDoc?.id == doc.id) {
                closeDocument()
            }
            loadDocuments()
        }
    }

    fun duplicateDocument(doc: PdfDocumentItem) {
        viewModelScope.launch {
            repository.duplicateDocument(doc)
            loadDocuments()
        }
    }

    // --- Active Document Management ---

    fun openDocument(doc: PdfDocumentItem) {
        viewModelScope.launch {
            activeRendererHelper?.close()
            val file = File(doc.filePath)
            val helper = PdfRendererHelper(file)
            activeRendererHelper = helper
            val count = helper.pageCount

            val stored = repository.loadAnnotations(doc.id)

            undoStack.clear()
            redoStack.clear()

            _uiState.update {
                it.copy(
                    activeDoc = doc,
                    currentPage = doc.lastOpenedPage.coerceIn(0, (count - 1).coerceAtLeast(0)),
                    totalPages = count,
                    textAnnotations = stored.texts,
                    bookmarks = stored.bookmarks,
                    imageAnnotations = emptyList(),
                    drawingAnnotations = emptyList(),
                    shapeAnnotations = emptyList(),
                    signatureAnnotations = emptyList(),
                    pageRotations = emptyMap(),
                    deletedPages = emptySet(),
                    activeTool = EditorTool.READ,
                    zoomScale = 1.0f
                )
            }
        }
    }

    fun closeDocument() {
        val active = _uiState.value.activeDoc
        if (active != null) {
            repository.saveLastOpenedPage(active.id, _uiState.value.currentPage)
            saveCurrentAnnotations()
        }
        activeRendererHelper?.close()
        activeRendererHelper = null
        _uiState.update { it.copy(activeDoc = null) }
    }

    fun renderCurrentPageBitmap(pageIndex: Int, targetWidth: Int = 1080): Bitmap? {
        val helper = activeRendererHelper ?: return null
        val rot = _uiState.value.pageRotations[pageIndex] ?: 0
        return helper.renderPage(pageIndex, targetWidth, rot)
    }

    fun setCurrentPage(page: Int) {
        val max = (_uiState.value.totalPages - 1).coerceAtLeast(0)
        val validPage = page.coerceIn(0, max)
        _uiState.update { it.copy(currentPage = validPage) }
        _uiState.value.activeDoc?.let {
            repository.saveLastOpenedPage(it.id, validPage)
        }
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _uiState.update { it.copy(readerTheme = theme) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setActiveTool(tool: EditorTool) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    fun setZoomScale(scale: Float) {
        val clamped = scale.coerceIn(0.75f, 4.0f)
        _uiState.update { it.copy(zoomScale = clamped) }
    }

    // --- Customization & Annotation Operations ---

    private fun pushUndoState() {
        undoStack.add(_uiState.value)
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value)
            _uiState.update {
                it.copy(
                    textAnnotations = prev.textAnnotations,
                    imageAnnotations = prev.imageAnnotations,
                    drawingAnnotations = prev.drawingAnnotations,
                    shapeAnnotations = prev.shapeAnnotations,
                    signatureAnnotations = prev.signatureAnnotations,
                    pageRotations = prev.pageRotations,
                    deletedPages = prev.deletedPages
                )
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value)
            _uiState.update {
                it.copy(
                    textAnnotations = next.textAnnotations,
                    imageAnnotations = next.imageAnnotations,
                    drawingAnnotations = next.drawingAnnotations,
                    shapeAnnotations = next.shapeAnnotations,
                    signatureAnnotations = next.signatureAnnotations,
                    pageRotations = next.pageRotations,
                    deletedPages = next.deletedPages
                )
            }
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // Add Text Annotation
    fun addTextAnnotation(
        text: String,
        fontSizeSp: Float,
        textColorHex: Long,
        bgColorHex: Long,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        fontFamilyName: String
    ) {
        pushUndoState()
        val newText = TextAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = _uiState.value.currentPage,
            text = text,
            xPercent = 0.20f,
            yPercent = 0.25f,
            fontSizeSp = fontSizeSp,
            textColorHex = textColorHex,
            bgColorHex = bgColorHex,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            fontFamilyName = fontFamilyName
        )
        _uiState.update { it.copy(textAnnotations = it.textAnnotations + newText) }
        saveCurrentAnnotations()
    }

    fun updateTextAnnotationPosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val updated = state.textAnnotations.map {
                if (it.id == id) it.copy(xPercent = newX.coerceIn(0.02f, 0.95f), yPercent = newY.coerceIn(0.02f, 0.95f))
                else it
            }
            state.copy(textAnnotations = updated)
        }
    }

    fun deleteTextAnnotation(id: String) {
        pushUndoState()
        _uiState.update { it.copy(textAnnotations = it.textAnnotations.filter { t -> t.id != id }) }
        saveCurrentAnnotations()
    }

    // Add Image / Stamp Annotation
    fun addStampAnnotation(stamp: StampType) {
        pushUndoState()
        val newStamp = ImageAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = _uiState.value.currentPage,
            imageFilePath = null,
            xPercent = 0.35f,
            yPercent = 0.35f,
            widthPercent = if (stamp.isCircular) 0.26f else 0.40f,
            heightPercent = if (stamp.isCircular) 0.20f else 0.10f,
            isStamp = true,
            stampType = stamp
        )
        _uiState.update { it.copy(imageAnnotations = it.imageAnnotations + newStamp) }
    }

    fun addCustomImageAnnotation(imageUri: Uri) {
        viewModelScope.launch {
            val localPath = repository.saveTemporaryImage(imageUri) ?: return@launch
            pushUndoState()
            val newImg = ImageAnnotation(
                id = UUID.randomUUID().toString(),
                pageIndex = _uiState.value.currentPage,
                imageFilePath = localPath,
                xPercent = 0.30f,
                yPercent = 0.30f,
                widthPercent = 0.40f,
                heightPercent = 0.25f,
                isStamp = false
            )
            _uiState.update { it.copy(imageAnnotations = it.imageAnnotations + newImg) }
        }
    }

    fun updateImageAnnotationPosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val updated = state.imageAnnotations.map {
                if (it.id == id) it.copy(xPercent = newX.coerceIn(0.02f, 0.95f), yPercent = newY.coerceIn(0.02f, 0.95f))
                else it
            }
            state.copy(imageAnnotations = updated)
        }
    }

    fun deleteImageAnnotation(id: String) {
        pushUndoState()
        _uiState.update { it.copy(imageAnnotations = it.imageAnnotations.filter { img -> img.id != id }) }
    }

    // Add Signature Annotation
    fun addSignatureAnnotation(strokes: List<List<DrawingPoint>>, strokeColorHex: Long) {
        pushUndoState()
        val newSig = SignatureAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = _uiState.value.currentPage,
            points = strokes,
            xPercent = 0.40f,
            yPercent = 0.70f,
            widthPercent = 0.38f,
            heightPercent = 0.12f,
            strokeColorHex = strokeColorHex
        )
        _uiState.update { it.copy(signatureAnnotations = it.signatureAnnotations + newSig) }
    }

    fun updateSignaturePosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val updated = state.signatureAnnotations.map {
                if (it.id == id) it.copy(xPercent = newX.coerceIn(0.02f, 0.95f), yPercent = newY.coerceIn(0.02f, 0.95f))
                else it
            }
            state.copy(signatureAnnotations = updated)
        }
    }

    fun deleteSignatureAnnotation(id: String) {
        pushUndoState()
        _uiState.update { it.copy(signatureAnnotations = it.signatureAnnotations.filter { s -> s.id != id }) }
    }

    // Add Drawing Annotation
    fun addDrawingStroke(points: List<DrawingPoint>) {
        if (points.size < 2) return
        pushUndoState()
        val newDrawing = DrawingAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = _uiState.value.currentPage,
            points = points,
            strokeWidth = _uiState.value.drawStrokeWidth,
            strokeColorHex = _uiState.value.drawStrokeColor,
            opacity = if (_uiState.value.isHighlighter) 0.35f else 1.0f,
            isHighlighter = _uiState.value.isHighlighter
        )
        _uiState.update { it.copy(drawingAnnotations = it.drawingAnnotations + newDrawing) }
    }

    fun setDrawStrokeSettings(color: Long, width: Float, isHighlighter: Boolean) {
        _uiState.update {
            it.copy(
                drawStrokeColor = color,
                drawStrokeWidth = width,
                isHighlighter = isHighlighter
            )
        }
    }

    // Add Shape Annotation
    fun addShapeAnnotation(shapeType: ShapeType, strokeColorHex: Long, strokeWidth: Float, hasFill: Boolean) {
        pushUndoState()
        val newShape = ShapeAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = _uiState.value.currentPage,
            shapeType = shapeType,
            startXPercent = 0.20f,
            startYPercent = 0.30f,
            endXPercent = 0.70f,
            endYPercent = 0.50f,
            strokeColorHex = strokeColorHex,
            fillColorHex = if (hasFill) (strokeColorHex and 0x00FFFFFF) or 0x33000000 else 0L,
            strokeWidth = strokeWidth
        )
        _uiState.update { it.copy(shapeAnnotations = it.shapeAnnotations + newShape) }
    }

    // Bookmarks
    fun toggleBookmarkCurrentPage() {
        val curr = _uiState.value.currentPage
        val existing = _uiState.value.bookmarks.find { it.pageIndex == curr }
        if (existing != null) {
            _uiState.update { it.copy(bookmarks = it.bookmarks.filter { b -> b.pageIndex != curr }) }
        } else {
            val newBm = BookmarkItem(
                id = UUID.randomUUID().toString(),
                pageIndex = curr,
                title = "Bookmark Page ${curr + 1}"
            )
            _uiState.update { it.copy(bookmarks = it.bookmarks + newBm) }
        }
        saveCurrentAnnotations()
    }

    fun applyPageModifications(rotations: Map<Int, Int>, deleted: Set<Int>) {
        pushUndoState()
        _uiState.update {
            it.copy(
                pageRotations = rotations,
                deletedPages = deleted
            )
        }
        activeRendererHelper?.clearCache()
    }

    private fun saveCurrentAnnotations() {
        val doc = _uiState.value.activeDoc ?: return
        viewModelScope.launch {
            repository.saveAnnotations(
                doc.id,
                PdfRepository.StoredAnnotations(
                    texts = _uiState.value.textAnnotations,
                    bookmarks = _uiState.value.bookmarks
                )
            )
        }
    }

    // --- Export / Flatten / Share ---

    fun exportFlattenedPdf(context: Context, onComplete: (File?) -> Unit) {
        val activeDoc = _uiState.value.activeDoc ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportSuccessMessage = null) }

            val exportDir = File(context.filesDir, "exported_pdfs").apply { mkdirs() }
            val cleanTitle = activeDoc.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val targetFile = File(exportDir, "${cleanTitle}_edited_${System.currentTimeMillis()}.pdf")

            val success = withContext(Dispatchers.IO) {
                PdfExporter.exportDocument(
                    context = context,
                    inputFile = File(activeDoc.filePath),
                    outputFile = targetFile,
                    textAnnotations = _uiState.value.textAnnotations,
                    imageAnnotations = _uiState.value.imageAnnotations,
                    drawingAnnotations = _uiState.value.drawingAnnotations,
                    shapeAnnotations = _uiState.value.shapeAnnotations,
                    signatureAnnotations = _uiState.value.signatureAnnotations,
                    pageRotations = _uiState.value.pageRotations,
                    deletedPages = _uiState.value.deletedPages
                )
            }

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportSuccessMessage = if (success) "PDF exported and saved successfully!" else "Failed to export PDF"
                )
            }

            if (success) {
                loadDocuments()
                onComplete(targetFile)
            } else {
                onComplete(null)
            }
        }
    }

    fun shareExportedPdf(context: Context) {
        exportFlattenedPdf(context) { file ->
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Customized PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        }
    }

    fun printDocument(context: Context) {
        exportFlattenedPdf(context) { file ->
            if (file != null && file.exists()) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder(file.name)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(_uiState.value.totalPages)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            file.inputStream().use { input ->
                                destination?.let { dest ->
                                    java.io.FileOutputStream(dest.fileDescriptor).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager?.print(file.nameWithoutExtension, printAdapter, PrintAttributes.Builder().build())
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        activeRendererHelper?.close()
    }
}
