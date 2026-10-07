package com.example.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.engine.PdfRendererHelper
import com.example.engine.SamplePdfGenerator
import com.example.model.BookmarkItem
import com.example.model.DrawingAnnotation
import com.example.model.ImageAnnotation
import com.example.model.PdfDocumentItem
import com.example.model.ShapeAnnotation
import com.example.model.SignatureAnnotation
import com.example.model.TextAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class PdfRepository(private val context: Context) {

    private val userPdfsDir = File(context.filesDir, "user_pdfs").apply { mkdirs() }
    private val annotationsDir = File(context.filesDir, "annotations").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("pdf_studio_prefs", Context.MODE_PRIVATE)

    suspend fun getDocuments(): List<PdfDocumentItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PdfDocumentItem>()

        // 1. Ensure sample PDFs exist
        val sampleFiles = SamplePdfGenerator.generateSamplePdfs(context)
        for (file in sampleFiles) {
            val docItem = fileToDocItem(file, isSample = true)
            list.add(docItem)
        }

        // 2. User imported PDFs
        val userFiles = userPdfsDir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }
        userFiles?.sortedByDescending { it.lastModified() }?.forEach { file ->
            list.add(fileToDocItem(file, isSample = false))
        }

        list.sortedWith(compareByDescending<PdfDocumentItem> { it.isFavorite }.thenByDescending { it.modifiedAt })
    }

    private fun fileToDocItem(file: File, isSample: Boolean): PdfDocumentItem {
        val id = file.nameWithoutExtension
        val title = file.nameWithoutExtension.replace('_', ' ')
        val isFav = prefs.getBoolean("fav_$id", false)
        val lastPage = prefs.getInt("last_page_$id", 0)

        // Count pages using lightweight renderer
        val helper = PdfRendererHelper(file)
        val count = helper.pageCount
        helper.close()

        return PdfDocumentItem(
            id = id,
            title = title,
            filePath = file.absolutePath,
            pageCount = count,
            fileSize = file.length(),
            modifiedAt = file.lastModified(),
            isFavorite = isFav,
            isSample = isSample,
            lastOpenedPage = lastPage
        )
    }

    suspend fun importPdfFromUri(uri: Uri, originalName: String?): PdfDocumentItem? = withContext(Dispatchers.IO) {
        try {
            val safeName = (originalName ?: "Imported_Doc_${System.currentTimeMillis()}.pdf")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetName = if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
            val targetFile = File(userPdfsDir, targetName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            fileToDocItem(targetFile, isSample = false)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createBlankDocument(title: String, pageCount: Int = 1): PdfDocumentItem = withContext(Dispatchers.IO) {
        val safeName = title.replace("[^a-zA-Z0-9._-]".toRegex(), "_") + ".pdf"
        val targetFile = File(userPdfsDir, safeName)

        val doc = PdfDocument()
        for (i in 1..pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i).create()
            val page = doc.startPage(pageInfo)
            // Clean white page
            page.canvas.drawColor(Color.WHITE)
            // Subtle header guide
            page.canvas.drawText("Document: $title", 40f, 40f, Paint().apply {
                color = Color.LTGRAY
                textSize = 9f
            })
            doc.finishPage(page)
        }
        FileOutputStream(targetFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        fileToDocItem(targetFile, isSample = false)
    }

    fun toggleFavorite(docId: String): Boolean {
        val current = prefs.getBoolean("fav_$docId", false)
        prefs.edit().putBoolean("fav_$docId", !current).apply()
        return !current
    }

    fun saveLastOpenedPage(docId: String, page: Int) {
        prefs.edit().putInt("last_page_$docId", page).apply()
    }

    suspend fun deleteDocument(doc: PdfDocumentItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(doc.filePath)
        val deleted = if (file.exists()) file.delete() else false
        // Clean annotations
        File(annotationsDir, "${doc.id}.json").delete()
        deleted
    }

    suspend fun duplicateDocument(doc: PdfDocumentItem): PdfDocumentItem? = withContext(Dispatchers.IO) {
        val srcFile = File(doc.filePath)
        if (!srcFile.exists()) return@withContext null

        val newFile = File(userPdfsDir, "${doc.id}_copy_${System.currentTimeMillis()}.pdf")
        srcFile.copyTo(newFile, overwrite = true)
        fileToDocItem(newFile, isSample = false)
    }

    // --- Annotation Storage & Retrieval ---

    data class StoredAnnotations(
        val texts: List<TextAnnotation> = emptyList(),
        val images: List<ImageAnnotation> = emptyList(),
        val drawings: List<DrawingAnnotation> = emptyList(),
        val shapes: List<ShapeAnnotation> = emptyList(),
        val signatures: List<SignatureAnnotation> = emptyList(),
        val bookmarks: List<BookmarkItem> = emptyList()
    )

    suspend fun saveAnnotations(docId: String, annotations: StoredAnnotations) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()

            // Texts
            val textsArray = JSONArray()
            annotations.texts.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("page", t.pageIndex)
                    put("text", t.text)
                    put("x", t.xPercent)
                    put("y", t.yPercent)
                    put("fontSize", t.fontSizeSp)
                    put("textColor", t.textColorHex)
                    put("bgColor", t.bgColorHex)
                    put("bold", t.isBold)
                    put("italic", t.isItalic)
                    put("underline", t.isUnderline)
                    put("fontFamily", t.fontFamilyName)
                }
                textsArray.put(obj)
            }
            json.put("texts", textsArray)

            // Bookmarks
            val bmArray = JSONArray()
            annotations.bookmarks.forEach { b ->
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("page", b.pageIndex)
                    put("title", b.title)
                    put("timestamp", b.timestamp)
                }
                bmArray.put(obj)
            }
            json.put("bookmarks", bmArray)

            val file = File(annotationsDir, "$docId.json")
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadAnnotations(docId: String): StoredAnnotations = withContext(Dispatchers.IO) {
        val file = File(annotationsDir, "$docId.json")
        if (!file.exists()) return@withContext StoredAnnotations()

        try {
            val content = file.readText()
            val json = JSONObject(content)

            val texts = mutableListOf<TextAnnotation>()
            if (json.has("texts")) {
                val textsArr = json.getJSONArray("texts")
                for (i in 0 until textsArr.length()) {
                    val obj = textsArr.getJSONObject(i)
                    texts.add(
                        TextAnnotation(
                            id = obj.getString("id"),
                            pageIndex = obj.getInt("page"),
                            text = obj.getString("text"),
                            xPercent = obj.getDouble("x").toFloat(),
                            yPercent = obj.getDouble("y").toFloat(),
                            fontSizeSp = obj.optDouble("fontSize", 16.0).toFloat(),
                            textColorHex = obj.optLong("textColor", 0xFF0F172A),
                            bgColorHex = obj.optLong("bgColor", 0L),
                            isBold = obj.optBoolean("bold", false),
                            isItalic = obj.optBoolean("italic", false),
                            isUnderline = obj.optBoolean("underline", false),
                            fontFamilyName = obj.optString("fontFamily", "Default")
                        )
                    )
                }
            }

            val bookmarks = mutableListOf<BookmarkItem>()
            if (json.has("bookmarks")) {
                val bmArr = json.getJSONArray("bookmarks")
                for (i in 0 until bmArr.length()) {
                    val obj = bmArr.getJSONObject(i)
                    bookmarks.add(
                        BookmarkItem(
                            id = obj.getString("id"),
                            pageIndex = obj.getInt("page"),
                            title = obj.getString("title"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            StoredAnnotations(texts = texts, bookmarks = bookmarks)
        } catch (e: Exception) {
            e.printStackTrace()
            StoredAnnotations()
        }
    }

    fun saveTemporaryImage(uri: Uri): String? {
        return try {
            val file = File(context.cacheDir, "img_${UUID.randomUUID()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { out ->
                    input.copyTo(out)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
