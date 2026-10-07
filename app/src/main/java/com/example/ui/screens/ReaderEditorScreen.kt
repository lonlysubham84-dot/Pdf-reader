package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingPoint
import com.example.model.EditorTool
import com.example.model.ImageAnnotation
import com.example.model.ReaderTheme
import com.example.model.ShapeType
import com.example.model.SignatureAnnotation
import com.example.model.StampType
import com.example.model.TextAnnotation
import com.example.ui.components.ExportDialog
import com.example.ui.components.PageOrganizerDialog
import com.example.ui.components.ShapePickerDialog
import com.example.ui.components.SignaturePadDialog
import com.example.ui.components.StampPickerSheet
import com.example.ui.components.StampPreviewCard
import com.example.ui.components.TextAnnotationDialog
import com.example.viewmodel.PdfViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderEditorScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val doc = uiState.activeDoc ?: return

    BackHandler {
        viewModel.closeDocument()
        onBack()
    }

    // Modal dialog states
    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextAnnotation by remember { mutableStateOf<TextAnnotation?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showStampSheet by remember { mutableStateOf(false) }
    var showShapeDialog by remember { mutableStateOf(false) }
    var showPageOrganizerDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Visual media picker for inserting custom images/photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addCustomImageAnnotation(uri)
        }
    }

    // Pan and zoom states
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Active in-progress drawing points
    val activeStrokePoints = remember { mutableStateListOf<DrawingPoint>() }

    // Rendered page bitmap
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(doc.id, uiState.currentPage, uiState.pageRotations[uiState.currentPage]) {
        pageBitmap = viewModel.renderCurrentPageBitmap(uiState.currentPage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Page ${uiState.currentPage + 1} of ${uiState.totalPages}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.pageRotations[uiState.currentPage] != null && uiState.pageRotations[uiState.currentPage] != 0) {
                                Text(
                                    text = " • ${uiState.pageRotations[uiState.currentPage]}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeDocument()
                            onBack()
                        },
                        modifier = Modifier.testTag("topbar_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Mode Switcher Pill: READ vs EDIT
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (uiState.activeTool == EditorTool.READ) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.setActiveTool(EditorTool.READ) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("mode_read_toggle")
                            ) {
                                Text(
                                    text = "Read",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.activeTool == EditorTool.READ) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (uiState.activeTool != EditorTool.READ) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (uiState.activeTool == EditorTool.READ) viewModel.setActiveTool(EditorTool.TEXT)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("mode_edit_toggle")
                            ) {
                                Text(
                                    text = "Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.activeTool != EditorTool.READ) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Undo & Redo (active in edit mode)
                    if (uiState.activeTool != EditorTool.READ) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = viewModel.canUndo(),
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = viewModel.canRedo(),
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                        }
                    }

                    // Bookmark
                    IconButton(onClick = { viewModel.toggleBookmarkCurrentPage() }) {
                        val isBookmarked = uiState.bookmarks.any { it.pageIndex == uiState.currentPage }
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Export / Share
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export & Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (uiState.activeTool == EditorTool.READ) {
                ReaderControlBar(
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    zoomScale = uiState.zoomScale,
                    readerTheme = uiState.readerTheme,
                    onPageChange = { viewModel.setCurrentPage(it) },
                    onZoomChange = { viewModel.setZoomScale(it) },
                    onThemeChange = { viewModel.setReaderTheme(it) }
                )
            } else {
                EditorToolsBar(
                    activeTool = uiState.activeTool,
                    onSelectTool = { tool ->
                        viewModel.setActiveTool(tool)
                        when (tool) {
                            EditorTool.TEXT -> {
                                editingTextAnnotation = null
                                showTextDialog = true
                            }
                            EditorTool.IMAGE -> {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            EditorTool.STAMP -> showStampSheet = true
                            EditorTool.SIGN -> showSignatureDialog = true
                            EditorTool.SHAPE -> showShapeDialog = true
                            EditorTool.ORGANIZE -> showPageOrganizerDialog = true
                            else -> {}
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(uiState.readerTheme.bgHex))
                .pointerInput(uiState.activeTool) {
                    if (uiState.activeTool == EditorTool.READ) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (uiState.zoomScale * zoom).coerceIn(0.8f, 3.5f)
                            viewModel.setZoomScale(newZoom)
                            panOffsetX = (panOffsetX + pan.x).coerceIn(-400f * newZoom, 400f * newZoom)
                            panOffsetY = (panOffsetY + pan.y).coerceIn(-600f * newZoom, 600f * newZoom)
                        }
                    }
                }
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // Main PDF Viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = uiState.zoomScale
                        scaleY = uiState.zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                    },
                contentAlignment = Alignment.Center
            ) {
                // PDF Page Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .aspectRatio(595f / 842f)
                        .shadow(8.dp, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                        .testTag("pdf_page_canvas")
                ) {
                    // Page Image with Reader Theme Matrix Filter
                    if (pageBitmap != null) {
                        val colorFilter = when (uiState.readerTheme) {
                            ReaderTheme.DARK -> ColorFilter.colorMatrix(ColorMatrix().apply {
                                setToSaturation(0f)
                                // Invert RGB
                                val invertMatrix = floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                                set(ColorMatrix(invertMatrix))
                            })
                            ReaderTheme.SEPIA -> ColorFilter.tint(Color(0x33F8F1E5), androidx.compose.ui.graphics.BlendMode.Multiply)
                            ReaderTheme.EYE_CARE -> ColorFilter.tint(Color(0x1AE8F5E9), androidx.compose.ui.graphics.BlendMode.Multiply)
                            else -> null
                        }

                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${uiState.currentPage + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter
                        )
                    }

                    // Render All Existing Drawings for this page
                    val pageDrawings = uiState.drawingAnnotations.filter { it.pageIndex == uiState.currentPage }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (draw in pageDrawings) {
                            if (draw.points.size < 2) continue
                            val path = Path()
                            path.moveTo(draw.points[0].x * size.width, draw.points[0].y * size.height)
                            for (i in 1 until draw.points.size) {
                                path.lineTo(draw.points[i].x * size.width, draw.points[i].y * size.height)
                            }
                            drawPath(
                                path = path,
                                color = Color(draw.strokeColorHex).copy(
                                    alpha = if (draw.isHighlighter) 0.35f else draw.opacity
                                ),
                                style = Stroke(
                                    width = draw.strokeWidth * (if (draw.isHighlighter) 3f else 1f),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Render All Existing Shapes for this page
                    val pageShapes = uiState.shapeAnnotations.filter { it.pageIndex == uiState.currentPage }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (shape in pageShapes) {
                            val startX = shape.startXPercent * size.width
                            val startY = shape.startYPercent * size.height
                            val endX = shape.endXPercent * size.width
                            val endY = shape.endYPercent * size.height

                            val left = minOf(startX, endX)
                            val top = minOf(startY, endY)
                            val w = (maxOf(startX, endX) - left).coerceAtLeast(1f)
                            val h = (maxOf(startY, endY) - top).coerceAtLeast(1f)

                            when (shape.shapeType) {
                                ShapeType.RECTANGLE -> {
                                    if (shape.fillColorHex != 0L) {
                                        drawRect(
                                            color = Color(shape.fillColorHex),
                                            topLeft = Offset(left, top),
                                            size = androidx.compose.ui.geometry.Size(w, h)
                                        )
                                    }
                                    drawRect(
                                        color = Color(shape.strokeColorHex),
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(w, h),
                                        style = Stroke(width = shape.strokeWidth)
                                    )
                                }
                                ShapeType.CIRCLE -> {
                                    if (shape.fillColorHex != 0L) {
                                        drawOval(
                                            color = Color(shape.fillColorHex),
                                            topLeft = Offset(left, top),
                                            size = androidx.compose.ui.geometry.Size(w, h)
                                        )
                                    }
                                    drawOval(
                                        color = Color(shape.strokeColorHex),
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(w, h),
                                        style = Stroke(width = shape.strokeWidth)
                                    )
                                }
                                ShapeType.LINE, ShapeType.ARROW -> {
                                    drawLine(
                                        color = Color(shape.strokeColorHex),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = shape.strokeWidth
                                    )
                                }
                                ShapeType.REDACTION -> {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(w, h)
                                    )
                                }
                            }
                        }
                    }

                    // Render Images and Stamps Overlays
                    val pageImages = uiState.imageAnnotations.filter { it.pageIndex == uiState.currentPage }
                    pageImages.forEach { img ->
                        DraggableImageOverlay(
                            image = img,
                            isEditMode = uiState.activeTool != EditorTool.READ,
                            onPositionChange = { newX, newY ->
                                viewModel.updateImageAnnotationPosition(img.id, newX, newY)
                            },
                            onDelete = { viewModel.deleteImageAnnotation(img.id) }
                        )
                    }

                    // Render Signatures Overlays
                    val pageSigs = uiState.signatureAnnotations.filter { it.pageIndex == uiState.currentPage }
                    pageSigs.forEach { sig ->
                        DraggableSignatureOverlay(
                            signature = sig,
                            isEditMode = uiState.activeTool != EditorTool.READ,
                            onPositionChange = { newX, newY ->
                                viewModel.updateSignaturePosition(sig.id, newX, newY)
                            },
                            onDelete = { viewModel.deleteSignatureAnnotation(sig.id) }
                        )
                    }

                    // Render Text Blocks Overlays
                    val pageTexts = uiState.textAnnotations.filter { it.pageIndex == uiState.currentPage }
                    pageTexts.forEach { textAnn ->
                        DraggableTextOverlay(
                            annotation = textAnn,
                            isEditMode = uiState.activeTool != EditorTool.READ,
                            onPositionChange = { newX, newY ->
                                viewModel.updateTextAnnotationPosition(textAnn.id, newX, newY)
                            },
                            onEdit = {
                                editingTextAnnotation = textAnn
                                showTextDialog = true
                            },
                            onDelete = { viewModel.deleteTextAnnotation(textAnn.id) }
                        )
                    }

                    // Active Freehand Drawing Capture Layer (when DRAW tool is active)
                    if (uiState.activeTool == EditorTool.DRAW) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            activeStrokePoints.clear()
                                            activeStrokePoints.add(
                                                DrawingPoint(
                                                    x = (offset.x / size.width).coerceIn(0f, 1f),
                                                    y = (offset.y / size.height).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            activeStrokePoints.add(
                                                DrawingPoint(
                                                    x = (change.position.x / size.width).coerceIn(0f, 1f),
                                                    y = (change.position.y / size.height).coerceIn(0f, 1f)
                                                )
                                            )
                                        },
                                        onDragEnd = {
                                            if (activeStrokePoints.size > 1) {
                                                viewModel.addDrawingStroke(activeStrokePoints.toList())
                                            }
                                            activeStrokePoints.clear()
                                        }
                                    )
                                }
                        ) {
                            if (activeStrokePoints.size > 1) {
                                val path = Path()
                                path.moveTo(
                                    activeStrokePoints[0].x * size.width,
                                    activeStrokePoints[0].y * size.height
                                )
                                for (i in 1 until activeStrokePoints.size) {
                                    path.lineTo(
                                        activeStrokePoints[i].x * size.width,
                                        activeStrokePoints[i].y * size.height
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color(uiState.drawStrokeColor).copy(
                                        alpha = if (uiState.isHighlighter) 0.35f else 1.0f
                                    ),
                                    style = Stroke(
                                        width = uiState.drawStrokeWidth * (if (uiState.isHighlighter) 3f else 1f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick Floating Reset Zoom button (if zoomed in)
            if (uiState.zoomScale != 1.0f || panOffsetX != 0f || panOffsetY != 0f) {
                IconButton(
                    onClick = {
                        viewModel.setZoomScale(1.0f)
                        panOffsetX = 0f
                        panOffsetY = 0f
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Zoom")
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showTextDialog) {
        TextAnnotationDialog(
            initialAnnotation = editingTextAnnotation,
            onDismiss = {
                showTextDialog = false
                editingTextAnnotation = null
            },
            onConfirm = { text, size, color, bg, bold, italic, underline, font ->
                if (editingTextAnnotation != null) {
                    // Update existing
                    viewModel.deleteTextAnnotation(editingTextAnnotation!!.id)
                }
                viewModel.addTextAnnotation(text, size, color, bg, bold, italic, underline, font)
                showTextDialog = false
                editingTextAnnotation = null
            }
        )
    }

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onConfirm = { strokes, color ->
                viewModel.addSignatureAnnotation(strokes, color)
                showSignatureDialog = false
            }
        )
    }

    if (showStampSheet) {
        StampPickerSheet(
            onDismiss = { showStampSheet = false },
            onStampSelected = { stamp ->
                viewModel.addStampAnnotation(stamp)
                showStampSheet = false
            }
        )
    }

    if (showShapeDialog) {
        ShapePickerDialog(
            onDismiss = { showShapeDialog = false },
            onConfirm = { shape, strokeColor, width, hasFill ->
                viewModel.addShapeAnnotation(shape, strokeColor, width, hasFill)
                showShapeDialog = false
            }
        )
    }

    if (showPageOrganizerDialog) {
        PageOrganizerDialog(
            pageCount = uiState.totalPages,
            initialRotations = uiState.pageRotations,
            onDismiss = { showPageOrganizerDialog = false },
            onApply = { rotations, deleted ->
                viewModel.applyPageModifications(rotations, deleted)
                showPageOrganizerDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            docTitle = doc.title,
            annotationCount = uiState.textAnnotations.size + uiState.imageAnnotations.size +
                    uiState.drawingAnnotations.size + uiState.signatureAnnotations.size,
            isExporting = uiState.isExporting,
            exportSuccessMessage = uiState.exportSuccessMessage,
            onDismiss = { showExportDialog = false },
            onSaveToDevice = {
                viewModel.exportFlattenedPdf(context) {}
            },
            onShare = {
                viewModel.shareExportedPdf(context)
            },
            onPrint = {
                viewModel.printDocument(context)
            }
        )
    }
}

@Composable
fun DraggableTextOverlay(
    annotation: TextAnnotation,
    isEditMode: Boolean,
    onPositionChange: (Float, Float) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(annotation.xPercent) { mutableFloatStateOf(annotation.xPercent) }
    var offsetY by remember(annotation.yPercent) { mutableFloatStateOf(annotation.yPercent) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentW = maxWidth
        val parentH = maxHeight

        val pixelX = parentW * offsetX
        val pixelY = parentH * offsetY

        Box(
            modifier = Modifier
                .offset { IntOffset(pixelX.roundToPx(), pixelY.roundToPx()) }
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                RoundedCornerShape(4.dp)
                            )
                            .pointerInput(annotation.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaXPercent = dragAmount.x / parentW.toPx()
                                    val deltaYPercent = dragAmount.y / parentH.toPx()
                                    offsetX = (offsetX + deltaXPercent).coerceIn(0.01f, 0.95f)
                                    offsetY = (offsetY + deltaYPercent).coerceIn(0.01f, 0.95f)
                                    onPositionChange(offsetX, offsetY)
                                }
                            }
                    } else Modifier
                )
                .padding(2.dp)
        ) {
            val previewFontFamily = when (annotation.fontFamilyName.lowercase()) {
                "serif" -> FontFamily.Serif
                "mono" -> FontFamily.Monospace
                else -> FontFamily.Default
            }

            Box(
                modifier = if (annotation.bgColorHex != 0L) {
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(annotation.bgColorHex))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else Modifier
            ) {
                Text(
                    text = annotation.text,
                    color = Color(annotation.textColorHex),
                    fontSize = annotation.fontSizeSp.sp,
                    fontWeight = if (annotation.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (annotation.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (annotation.isUnderline) TextDecoration.Underline else TextDecoration.None,
                    fontFamily = previewFontFamily
                )
            }

            // Quick edit and delete badges in edit mode
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .offset(y = (-18).dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onEdit() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableImageOverlay(
    image: ImageAnnotation,
    isEditMode: Boolean,
    onPositionChange: (Float, Float) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(image.xPercent) { mutableFloatStateOf(image.xPercent) }
    var offsetY by remember(image.yPercent) { mutableFloatStateOf(image.yPercent) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentW = maxWidth
        val parentH = maxHeight

        val pixelX = parentW * offsetX
        val pixelY = parentH * offsetY
        val widthDp = parentW * image.widthPercent
        val heightDp = parentH * image.heightPercent

        Box(
            modifier = Modifier
                .offset { IntOffset(pixelX.roundToPx(), pixelY.roundToPx()) }
                .size(widthDp, heightDp)
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                RoundedCornerShape(6.dp)
                            )
                            .pointerInput(image.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaXPercent = dragAmount.x / parentW.toPx()
                                    val deltaYPercent = dragAmount.y / parentH.toPx()
                                    offsetX = (offsetX + deltaXPercent).coerceIn(0.01f, 0.95f)
                                    offsetY = (offsetY + deltaYPercent).coerceIn(0.01f, 0.95f)
                                    onPositionChange(offsetX, offsetY)
                                }
                            }
                    } else Modifier
                )
        ) {
            if (image.isStamp && image.stampType != null) {
                StampPreviewCard(stamp = image.stampType, onClick = {})
            } else if (image.imageFilePath != null) {
                val file = File(image.imageFilePath)
                if (file.exists()) {
                    val bmp = remember(file.absolutePath) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Custom Image Overlay",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableSignatureOverlay(
    signature: SignatureAnnotation,
    isEditMode: Boolean,
    onPositionChange: (Float, Float) -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember(signature.xPercent) { mutableFloatStateOf(signature.xPercent) }
    var offsetY by remember(signature.yPercent) { mutableFloatStateOf(signature.yPercent) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentW = maxWidth
        val parentH = maxHeight

        val pixelX = parentW * offsetX
        val pixelY = parentH * offsetY
        val widthDp = parentW * signature.widthPercent
        val heightDp = parentH * signature.heightPercent

        Box(
            modifier = Modifier
                .offset { IntOffset(pixelX.roundToPx(), pixelY.roundToPx()) }
                .size(widthDp, heightDp)
                .then(
                    if (isEditMode) {
                        Modifier
                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .pointerInput(signature.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaXPercent = dragAmount.x / parentW.toPx()
                                    val deltaYPercent = dragAmount.y / parentH.toPx()
                                    offsetX = (offsetX + deltaXPercent).coerceIn(0.01f, 0.95f)
                                    offsetY = (offsetY + deltaYPercent).coerceIn(0.01f, 0.95f)
                                    onPositionChange(offsetX, offsetY)
                                }
                            }
                    } else Modifier
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawColor = Color(signature.strokeColorHex)
                for (stroke in signature.points) {
                    if (stroke.size < 2) continue
                    val path = Path()
                    path.moveTo(stroke[0].x * size.width, stroke[0].y * size.height)
                    for (i in 1 until stroke.size) {
                        path.lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                    }
                    drawPath(
                        path = path,
                        color = drawColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderControlBar(
    currentPage: Int,
    totalPages: Int,
    zoomScale: Float,
    readerTheme: ReaderTheme,
    onPageChange: (Int) -> Unit,
    onZoomChange: (Float) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Page navigation slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPageChange(currentPage - 1) },
                    enabled = currentPage > 0,
                    modifier = Modifier.testTag("nav_prev_page")
                ) {
                    Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = "Previous Page")
                }

                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("page_slider")
                )

                IconButton(
                    onClick = { onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.testTag("nav_next_page")
                ) {
                    Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "Next Page")
                }
            }

            // Quick actions: Themes & Zoom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderTheme.values().forEach { theme ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(theme.bgHex))
                                .border(
                                    width = if (readerTheme == theme) 2.dp else 1.dp,
                                    color = if (readerTheme == theme) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { onThemeChange(theme) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (readerTheme == theme) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (theme == ReaderTheme.DARK) Color.White else Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Zoom Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onZoomChange((zoomScale - 0.25f).coerceAtLeast(0.75f)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onZoomChange((zoomScale + 0.25f).coerceAtMost(3.5f)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditorToolsBar(
    activeTool: EditorTool,
    onSelectTool: (EditorTool) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tools = listOf(
                EditorTool.TEXT to Icons.Default.TextFields,
                EditorTool.IMAGE to Icons.Default.Image,
                EditorTool.STAMP to Icons.Default.Verified,
                EditorTool.DRAW to Icons.Default.Brush,
                EditorTool.SIGN to Icons.Default.Gesture,
                EditorTool.SHAPE to Icons.Default.Layers,
                EditorTool.ORGANIZE to Icons.Default.MenuBook
            )

            items(tools.size) { idx ->
                val (tool, icon) = tools[idx]
                val isSelected = activeTool == tool

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTool(tool) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("tool_button_${tool.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = tool.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
