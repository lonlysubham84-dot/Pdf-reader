package com.example.model

enum class EditorTool(val title: String) {
    READ("Read"),
    TEXT("Add Text"),
    IMAGE("Add Image"),
    STAMP("Stamps"),
    DRAW("Draw & Pen"),
    SIGN("Signature"),
    SHAPE("Shapes"),
    ORGANIZE("Pages")
}

enum class ShapeType(val title: String) {
    RECTANGLE("Rectangle"),
    CIRCLE("Circle / Oval"),
    ARROW("Arrow"),
    LINE("Line"),
    REDACTION("Redaction Box")
}

enum class StampType(val label: String, val colorHex: Long, val isCircular: Boolean = false) {
    APPROVED("APPROVED", 0xFF10B981),
    CONFIDENTIAL("CONFIDENTIAL", 0xFFDC2626),
    URGENT("URGENT", 0xFFEA580C),
    DRAFT("DRAFT", 0xFFF59E0B),
    FINAL("FINAL COPY", 0xFF2563EB),
    PAID("PAID IN FULL", 0xFF059669),
    REVIEWED("REVIEWED", 0xFF7C3AED),
    OFFICIAL("OFFICIAL SEAL", 0xFF1E3A8A, true)
}

data class DrawingPoint(
    val x: Float, // 0.0f .. 1.0f relative to page width
    val y: Float  // 0.0f .. 1.0f relative to page height
)

data class DrawingAnnotation(
    val id: String,
    val pageIndex: Int,
    val points: List<DrawingPoint>,
    val strokeWidth: Float = 4f,
    val strokeColorHex: Long = 0xFFDC2626,
    val opacity: Float = 1.0f,
    val isHighlighter: Boolean = false
)

data class TextAnnotation(
    val id: String,
    val pageIndex: Int,
    val text: String,
    val xPercent: Float, // 0.0 .. 1.0
    val yPercent: Float, // 0.0 .. 1.0
    val fontSizeSp: Float = 16f,
    val textColorHex: Long = 0xFF0F172A,
    val bgColorHex: Long = 0x00000000, // Transparent by default
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontFamilyName: String = "Default" // Default, Serif, Sans, Mono
)

data class ImageAnnotation(
    val id: String,
    val pageIndex: Int,
    val imageFilePath: String?,
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float = 0.35f,
    val heightPercent: Float = 0.20f,
    val opacity: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val isStamp: Boolean = false,
    val stampType: StampType? = null
)

data class ShapeAnnotation(
    val id: String,
    val pageIndex: Int,
    val shapeType: ShapeType,
    val startXPercent: Float,
    val startYPercent: Float,
    val endXPercent: Float,
    val endYPercent: Float,
    val strokeColorHex: Long = 0xFF2563EB,
    val fillColorHex: Long = 0x00000000,
    val strokeWidth: Float = 4f
)

data class SignatureAnnotation(
    val id: String,
    val pageIndex: Int,
    val points: List<List<DrawingPoint>>,
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float = 0.35f,
    val heightPercent: Float = 0.15f,
    val strokeColorHex: Long = 0xFF0F172A
)

data class BookmarkItem(
    val id: String,
    val pageIndex: Int,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PageModification(
    val originalPageIndex: Int,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val isDeleted: Boolean = false
)
