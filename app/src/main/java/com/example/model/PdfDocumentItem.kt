package com.example.model

data class PdfDocumentItem(
    val id: String,
    val title: String,
    val filePath: String,
    val pageCount: Int,
    val fileSize: Long,
    val modifiedAt: Long,
    val isFavorite: Boolean = false,
    val isSample: Boolean = false,
    val lastOpenedPage: Int = 0
)

enum class ReaderTheme(val title: String, val bgHex: Long, val textHex: Long) {
    LIGHT("Day White", 0xFFFFFFFF, 0xFF0F172A),
    DARK("Night Invert", 0xFF121212, 0xFFE2E8F0),
    SEPIA("Warm Paper", 0xFFFBF0D9, 0xFF4A3728),
    EYE_CARE("Eye Comfort", 0xFFE8F5E9, 0xFF1B5E20)
}

enum class ViewMode(val title: String) {
    SINGLE("Single Page"),
    CONTINUOUS("Scroll View")
}
