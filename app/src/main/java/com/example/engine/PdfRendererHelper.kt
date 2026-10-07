package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.geometry.Size
import java.io.File

class PdfRendererHelper(private val file: File) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 6 // Use 1/6th of available memory for cache
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        initRenderer()
    }

    private fun initRenderer() {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    fun getPageSize(pageIndex: Int): Size {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) {
            return Size(595f, 842f) // Default A4
        }
        synchronized(this) {
            var page: PdfRenderer.Page? = null
            return try {
                page = pdfRenderer?.openPage(pageIndex)
                if (page != null) {
                    Size(page.width.toFloat(), page.height.toFloat())
                } else {
                    Size(595f, 842f)
                }
            } catch (e: Exception) {
                Size(595f, 842f)
            } finally {
                page?.close()
            }
        }
    }

    fun renderPage(pageIndex: Int, targetWidth: Int = 1080, rotationDeg: Int = 0): Bitmap? {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return null

        val cacheKey = "p_${pageIndex}_w_${targetWidth}_r_$rotationDeg"
        memoryCache.get(cacheKey)?.let {
            if (!it.isRecycled) return it
        }

        synchronized(this) {
            var page: PdfRenderer.Page? = null
            return try {
                page = pdfRenderer?.openPage(pageIndex) ?: return null
                val originalW = page.width
                val originalH = page.height

                val scale = targetWidth.toFloat() / originalW.toFloat()
                val targetHeight = (originalH * scale).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                // Fill white background for PDF page
                bitmap.eraseColor(Color.WHITE)

                val matrix = Matrix()
                matrix.postScale(scale, scale)

                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val finalBitmap = if (rotationDeg != 0) {
                    val rotMatrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotMatrix, true)
                    bitmap.recycle()
                    rotated
                } else {
                    bitmap
                }

                memoryCache.put(cacheKey, finalBitmap)
                finalBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                page?.close()
            }
        }
    }

    fun clearCache() {
        memoryCache.evictAll()
    }

    fun close() {
        clearCache()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
    }
}
