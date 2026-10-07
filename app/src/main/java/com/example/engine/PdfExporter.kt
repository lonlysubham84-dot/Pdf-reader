package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.DrawingAnnotation
import com.example.model.ImageAnnotation
import com.example.model.ShapeAnnotation
import com.example.model.ShapeType
import com.example.model.SignatureAnnotation
import com.example.model.StampType
import com.example.model.TextAnnotation
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PdfExporter {

    fun exportDocument(
        context: Context,
        inputFile: File,
        outputFile: File,
        textAnnotations: List<TextAnnotation>,
        imageAnnotations: List<ImageAnnotation>,
        drawingAnnotations: List<DrawingAnnotation>,
        shapeAnnotations: List<ShapeAnnotation>,
        signatureAnnotations: List<SignatureAnnotation>,
        pageRotations: Map<Int, Int> = emptyMap(),
        deletedPages: Set<Int> = emptySet()
    ): Boolean {
        var rendererHelper: PdfRendererHelper? = null
        val doc = PdfDocument()

        return try {
            rendererHelper = PdfRendererHelper(inputFile)
            val totalPages = rendererHelper.pageCount

            var outputPageNum = 1
            for (pageIndex in 0 until totalPages) {
                if (deletedPages.contains(pageIndex)) continue

                val rotation = pageRotations[pageIndex] ?: 0
                val pageSize = rendererHelper.getPageSize(pageIndex)

                val pageW = if (rotation == 90 || rotation == 270) pageSize.height.toInt() else pageSize.width.toInt()
                val pageH = if (rotation == 90 || rotation == 270) pageSize.width.toInt() else pageSize.height.toInt()

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, outputPageNum).create()
                val pdfPage = doc.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Render high quality base bitmap
                val baseBitmap = rendererHelper.renderPage(pageIndex, targetWidth = pageW * 2, rotationDeg = rotation)
                if (baseBitmap != null) {
                    canvas.drawBitmap(baseBitmap, null, RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()), null)
                }

                // Draw drawings & highlighters
                drawDrawings(canvas, pageIndex, drawingAnnotations, pageW.toFloat(), pageH.toFloat())

                // Draw shapes and redactions
                drawShapes(canvas, pageIndex, shapeAnnotations, pageW.toFloat(), pageH.toFloat())

                // Draw images & stamps
                drawImages(canvas, pageIndex, imageAnnotations, pageW.toFloat(), pageH.toFloat())

                // Draw signatures
                drawSignatures(canvas, pageIndex, signatureAnnotations, pageW.toFloat(), pageH.toFloat())

                // Draw text overlays
                drawTexts(canvas, pageIndex, textAnnotations, pageW.toFloat(), pageH.toFloat())

                doc.finishPage(pdfPage)
                outputPageNum++
            }

            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                doc.close()
            } catch (_: Exception) {}
            rendererHelper?.close()
        }
    }

    private fun drawDrawings(canvas: Canvas, pageIndex: Int, drawings: List<DrawingAnnotation>, pageW: Float, pageH: Float) {
        val pageDrawings = drawings.filter { it.pageIndex == pageIndex }
        for (draw in pageDrawings) {
            if (draw.points.size < 2) continue

            val paint = Paint().apply {
                color = draw.strokeColorHex.toInt()
                alpha = if (draw.isHighlighter) (draw.opacity * 100).toInt() else (draw.opacity * 255).toInt()
                strokeWidth = draw.strokeWidth * (if (draw.isHighlighter) 3f else 1f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            val path = Path()
            val first = draw.points.first()
            path.moveTo(first.x * pageW, first.y * pageH)

            for (i in 1 until draw.points.size) {
                val pt = draw.points[i]
                path.lineTo(pt.x * pageW, pt.y * pageH)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawShapes(canvas: Canvas, pageIndex: Int, shapes: List<ShapeAnnotation>, pageW: Float, pageH: Float) {
        val pageShapes = shapes.filter { it.pageIndex == pageIndex }
        for (shape in pageShapes) {
            val startX = shape.startXPercent * pageW
            val startY = shape.startYPercent * pageH
            val endX = shape.endXPercent * pageW
            val endY = shape.endYPercent * pageH

            val strokePaint = Paint().apply {
                color = shape.strokeColorHex.toInt()
                strokeWidth = shape.strokeWidth
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val fillPaint = Paint().apply {
                color = shape.fillColorHex.toInt()
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val rect = RectF(
                minOf(startX, endX),
                minOf(startY, endY),
                maxOf(startX, endX),
                maxOf(startY, endY)
            )

            when (shape.shapeType) {
                ShapeType.RECTANGLE -> {
                    if (shape.fillColorHex != 0L) canvas.drawRect(rect, fillPaint)
                    canvas.drawRect(rect, strokePaint)
                }
                ShapeType.CIRCLE -> {
                    if (shape.fillColorHex != 0L) canvas.drawOval(rect, fillPaint)
                    canvas.drawOval(rect, strokePaint)
                }
                ShapeType.LINE -> {
                    canvas.drawLine(startX, startY, endX, endY, strokePaint)
                }
                ShapeType.ARROW -> {
                    canvas.drawLine(startX, startY, endX, endY, strokePaint)
                    drawArrowHead(canvas, startX, startY, endX, endY, strokePaint)
                }
                ShapeType.REDACTION -> {
                    // Solid black redaction box
                    val redactPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(rect, redactPaint)
                }
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, paint: Paint) {
        val arrowHeadLength = 16f
        val angle = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
        val arrowAngle = Math.PI / 6

        val x1 = (toX - arrowHeadLength * cos(angle - arrowAngle)).toFloat()
        val y1 = (toY - arrowHeadLength * sin(angle - arrowAngle)).toFloat()
        val x2 = (toX - arrowHeadLength * cos(angle + arrowAngle)).toFloat()
        val y2 = (toY - arrowHeadLength * sin(angle + arrowAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(toX, toY)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }
        val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
        canvas.drawPath(headPath, fillPaint)
    }

    private fun drawImages(canvas: Canvas, pageIndex: Int, images: List<ImageAnnotation>, pageW: Float, pageH: Float) {
        val pageImages = images.filter { it.pageIndex == pageIndex }
        for (img in pageImages) {
            val left = img.xPercent * pageW
            val top = img.yPercent * pageH
            val width = img.widthPercent * pageW
            val height = img.heightPercent * pageH

            canvas.save()
            if (img.rotationDeg != 0f) {
                canvas.rotate(img.rotationDeg, left + width / 2f, top + height / 2f)
            }

            if (img.isStamp && img.stampType != null) {
                drawStamp(canvas, img.stampType, left, top, width, height, img.opacity)
            } else if (img.imageFilePath != null) {
                val file = File(img.imageFilePath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        val paint = Paint().apply {
                            alpha = (img.opacity * 255).toInt()
                            isFilterBitmap = true
                        }
                        canvas.drawBitmap(bmp, null, RectF(left, top, left + width, top + height), paint)
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawStamp(canvas: Canvas, stampType: StampType, left: Float, top: Float, width: Float, height: Float, opacity: Float) {
        val rect = RectF(left, top, left + width, top + height)
        val color = stampType.colorHex.toInt()

        val borderPaint = Paint().apply {
            this.color = color
            alpha = (opacity * 240).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            this.color = color
            alpha = (opacity * 25).toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            this.color = color
            alpha = (opacity * 245).toInt()
            textSize = height * 0.38f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        if (stampType.isCircular) {
            canvas.drawOval(rect, fillPaint)
            canvas.drawOval(rect, borderPaint)
            val innerRect = RectF(rect.left + 5f, rect.top + 5f, rect.right - 5f, rect.bottom - 5f)
            canvas.drawOval(innerRect, Paint(borderPaint).apply { strokeWidth = 1.2f })
            canvas.drawText(stampType.label, rect.centerX(), rect.centerY() + textPaint.textSize * 0.35f, textPaint)
        } else {
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            val innerRect = RectF(rect.left + 4f, rect.top + 4f, rect.right - 4f, rect.bottom - 4f)
            canvas.drawRoundRect(innerRect, 6f, 6f, Paint(borderPaint).apply { strokeWidth = 1.2f })
            canvas.drawText(stampType.label, rect.centerX(), rect.centerY() + textPaint.textSize * 0.35f, textPaint)
        }
    }

    private fun drawSignatures(canvas: Canvas, pageIndex: Int, signatures: List<SignatureAnnotation>, pageW: Float, pageH: Float) {
        val pageSigs = signatures.filter { it.pageIndex == pageIndex }
        for (sig in pageSigs) {
            val left = sig.xPercent * pageW
            val top = sig.yPercent * pageH
            val width = sig.widthPercent * pageW
            val height = sig.heightPercent * pageH

            val paint = Paint().apply {
                color = sig.strokeColorHex.toInt()
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (stroke in sig.points) {
                if (stroke.size < 2) continue
                val path = Path()
                path.moveTo(left + stroke[0].x * width, top + stroke[0].y * height)
                for (i in 1 until stroke.size) {
                    path.lineTo(left + stroke[i].x * width, top + stroke[i].y * height)
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawTexts(canvas: Canvas, pageIndex: Int, texts: List<TextAnnotation>, pageW: Float, pageH: Float) {
        val pageTexts = texts.filter { it.pageIndex == pageIndex }
        for (t in pageTexts) {
            val x = t.xPercent * pageW
            val y = t.yPercent * pageH

            val typeface = when (t.fontFamilyName.lowercase()) {
                "serif" -> Typeface.SERIF
                "mono", "monospace" -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
            val style = when {
                t.isBold && t.isItalic -> Typeface.BOLD_ITALIC
                t.isBold -> Typeface.BOLD
                t.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            val textPaint = Paint().apply {
                color = t.textColorHex.toInt()
                textSize = t.fontSizeSp * 1.33f // Convert sp to pt approx
                this.typeface = Typeface.create(typeface, style)
                isUnderlineText = t.isUnderline
                isAntiAlias = true
            }

            val textWidth = textPaint.measureText(t.text)
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.bottom - fontMetrics.top

            // Background pill if defined
            if (t.bgColorHex != 0L) {
                val bgPaint = Paint().apply {
                    color = t.bgColorHex.toInt()
                    this.style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val pillRect = RectF(x - 6f, y + fontMetrics.top - 4f, x + textWidth + 6f, y + fontMetrics.bottom + 4f)
                canvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
            }

            canvas.drawText(t.text, x, y, textPaint)
        }
    }
}
