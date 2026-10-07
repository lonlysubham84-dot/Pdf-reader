package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {

    private const val PAGE_WIDTH = 595  // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)

    fun generateSamplePdfs(context: Context): List<File> {
        val pdfDir = File(context.filesDir, "sample_pdfs").apply { mkdirs() }
        val generatedFiles = mutableListOf<File>()

        val file1 = File(pdfDir, "Executive_Business_Agreement.pdf")
        if (!file1.exists() || file1.length() == 0L) {
            generateBusinessAgreement(file1)
        }
        generatedFiles.add(file1)

        val file2 = File(pdfDir, "Product_Architecture_Whitepaper.pdf")
        if (!file2.exists() || file2.length() == 0L) {
            generateArchitectureWhitepaper(file2)
        }
        generatedFiles.add(file2)

        val file3 = File(pdfDir, "Creative_Services_Invoice.pdf")
        if (!file3.exists() || file3.length() == 0L) {
            generateInvoice(file3)
        }
        generatedFiles.add(file3)

        return generatedFiles
    }

    private fun generateBusinessAgreement(targetFile: File) {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val accentFillPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // --- PAGE 1: Intro & Scope ---
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = doc.startPage(pageInfo1)
        var canvas = page1.canvas

        // Header Banner
        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 12f), Paint().apply { color = Color.rgb(37, 99, 235) })
        canvas.drawText("CONFIDENTIAL & PROPRIETARY", 40f, 45f, Paint().apply {
            color = Color.rgb(220, 38, 38)
            textSize = 9f
            isFakeBoldText = true
        })

        canvas.drawText("MUTUAL NON-DISCLOSURE AGREEMENT", 40f, 75f, titlePaint)
        canvas.drawText("Reference No: NDA-2026-X89 • Effective Date: October 2026", 40f, 95f, subtitlePaint)
        canvas.drawLine(40f, 110f, (PAGE_WIDTH - 40).toFloat(), 110f, linePaint)

        // Summary box
        canvas.drawRoundRect(RectF(40f, 125f, (PAGE_WIDTH - 40).toFloat(), 215f), 8f, 8f, accentFillPaint)
        canvas.drawText("PARTIES TO THIS AGREEMENT", 55f, 150f, headerPaint)
        canvas.drawText("1. Disclosing Party: Apex Global Ventures Inc., Delaware Corporation", 55f, 172f, bodyPaint)
        canvas.drawText("2. Receiving Party: Nexus Enterprise Solutions LLC, California", 55f, 192f, bodyPaint)

        canvas.drawText("1. PURPOSE OF DISCLOSURE", 40f, 245f, headerPaint)
        val text1 = listOf(
            "The Disclosing Party agrees to share proprietary technical specifications, architectural designs,",
            "and business projections solely for evaluating prospective strategic partnerships and product integrations.",
            "Neither party is granted any intellectual property license beyond this explicit evaluation purpose."
        )
        var curY = 265f
        for (line in text1) {
            canvas.drawText(line, 40f, curY, bodyPaint)
            curY += 16f
        }

        curY += 15f
        canvas.drawText("2. DEFINITION OF CONFIDENTIAL INFORMATION", 40f, curY, headerPaint)
        curY += 20f
        val text2 = listOf(
            "\"Confidential Information\" includes all non-public technical, economic, or marketing knowledge disclosed",
            "verbally, in written documents, digital media, source code, schematics, customer records, and trade secrets.",
            "Information marked as 'CONFIDENTIAL' or understood to be proprietary shall be governed by these covenants.",
            "Exceptions apply strictly to information independently discovered or already in the public domain."
        )
        for (line in text2) {
            canvas.drawText(line, 40f, curY, bodyPaint)
            curY += 16f
        }

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Apex Global Ventures • Page 1 of 3", 40f, 800f, subtitlePaint)
        doc.finishPage(page1)

        // --- PAGE 2: Obligations & Term ---
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = doc.startPage(pageInfo2)
        canvas = page2.canvas

        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 12f), Paint().apply { color = Color.rgb(37, 99, 235) })
        canvas.drawText("SECTION 3: STANDARD OF CARE & OBLIGATIONS", 40f, 50f, headerPaint)
        canvas.drawLine(40f, 65f, (PAGE_WIDTH - 40).toFloat(), 65f, linePaint)

        curY = 90f
        val text3 = listOf(
            "The Receiving Party warrants and agrees to hold all Confidential Information in the strictest confidence,",
            "employing at least the degree of care it exercises with its own high-security trade secrets (and in no case",
            "less than reasonable care). Access is restricted exclusively to authorized employees and legal advisors.",
            "Disclosure to third parties without prior written consent constitutes a material breach.",
            "",
            "SECTION 4: RETURN OR DESTRUCTION OF MATERIALS",
            "Upon written demand by Disclosing Party or upon termination of discussions, the Receiving Party shall",
            "promptly return or certify destruction of all physical copies, digital records, abstracts, and derivatives.",
            "",
            "SECTION 5: TERM AND DURATION",
            "The confidentiality obligations under this Agreement shall survive for five (5) years from disclosure date,",
            "provided that trade secrets shall remain confidential in perpetuity under applicable uniform trade secrets acts."
        )
        for (line in text3) {
            if (line.startsWith("SECTION")) {
                curY += 10f
                canvas.drawText(line, 40f, curY, headerPaint)
                curY += 20f
            } else if (line.isNotEmpty()) {
                canvas.drawText(line, 40f, curY, bodyPaint)
                curY += 16f
            }
        }

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Apex Global Ventures • Page 2 of 3", 40f, 800f, subtitlePaint)
        doc.finishPage(page2)

        // --- PAGE 3: Signatures & Stamp Area ---
        val pageInfo3 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create()
        val page3 = doc.startPage(pageInfo3)
        canvas = page3.canvas

        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 12f), Paint().apply { color = Color.rgb(37, 99, 235) })
        canvas.drawText("SECTION 6: EXECUTION & BINDING SIGNATURES", 40f, 50f, headerPaint)
        canvas.drawLine(40f, 65f, (PAGE_WIDTH - 40).toFloat(), 65f, linePaint)

        canvas.drawText("IN WITNESS WHEREOF, the duly authorized representatives execute this Agreement as of the Effective Date.", 40f, 90f, bodyPaint)

        // Signature Blocks
        val sigBoxWidth = (PAGE_WIDTH - 100) / 2f
        // Left party
        canvas.drawRoundRect(RectF(40f, 130f, 40f + sigBoxWidth, 270f), 6f, 6f, accentFillPaint)
        canvas.drawText("DISCLOSING PARTY:", 55f, 155f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 11f; isFakeBoldText = true })
        canvas.drawText("Apex Global Ventures Inc.", 55f, 175f, bodyPaint)
        canvas.drawLine(55f, 230f, 40f + sigBoxWidth - 15f, 230f, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
        canvas.drawText("Authorized Signatory: Sarah Jenkins, VP Legal", 55f, 245f, Paint().apply { color = Color.DKGRAY; textSize = 9f })

        // Right party
        val rightX = 60f + sigBoxWidth
        canvas.drawRoundRect(RectF(rightX, 130f, rightX + sigBoxWidth, 270f), 6f, 6f, accentFillPaint)
        canvas.drawText("RECEIVING PARTY:", rightX + 15f, 155f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 11f; isFakeBoldText = true })
        canvas.drawText("Nexus Enterprise Solutions LLC", rightX + 15f, 175f, bodyPaint)
        canvas.drawLine(rightX + 15f, 230f, rightX + sigBoxWidth - 15f, 230f, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
        canvas.drawText("Authorized Signatory: [Sign Here With PDF Studio]", rightX + 15f, 245f, Paint().apply { color = Color.rgb(37, 99, 235); textSize = 9f; isFakeBoldText = true })

        // Note callout
        canvas.drawRoundRect(RectF(40f, 320f, (PAGE_WIDTH - 40).toFloat(), 390f), 6f, 6f, Paint().apply {
            color = Color.rgb(254, 243, 199)
            style = Paint.Style.FILL
        })
        canvas.drawText("TIP: You can use the Sign, Text, or Stamp tools above to add your digital signature", 55f, 350f, Paint().apply {
            color = Color.rgb(180, 83, 9)
            textSize = 10f
            isFakeBoldText = true
        })
        canvas.drawText("and official corporate seal right onto this contract!", 55f, 370f, Paint().apply {
            color = Color.rgb(180, 83, 9)
            textSize = 10f
        })

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Apex Global Ventures • Page 3 of 3", 40f, 800f, subtitlePaint)
        doc.finishPage(page3)

        FileOutputStream(targetFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()
    }

    private fun generateArchitectureWhitepaper(targetFile: File) {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }

        // --- PAGE 1 ---
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = doc.startPage(pageInfo1)
        var canvas = page1.canvas

        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 12f), Paint().apply { color = Color.rgb(16, 185, 129) })
        canvas.drawText("SYSTEM ARCHITECTURE & CLOUD WHITEPAPER", 40f, 60f, titlePaint)
        canvas.drawText("High-Concurrency Distributed Microservices • v4.2 Release", 40f, 82f, Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 11f
        })
        canvas.drawLine(40f, 100f, (PAGE_WIDTH - 40).toFloat(), 100f, linePaint)

        canvas.drawText("1. EXECUTIVE SUMMARY", 40f, 130f, headerPaint)
        val p1 = listOf(
            "This document outlines the zero-trust cloud infrastructure supporting 10,000+ real-time transactions",
            "per second with 99.999% SLA availability. Key pillars include multi-region edge caches, asynchronous",
            "event streams via Apache Kafka, and automated Kubernetes orchestration with auto-scaling triggers."
        )
        var curY = 150f
        for (line in p1) {
            canvas.drawText(line, 40f, curY, bodyPaint)
            curY += 16f
        }

        // Architecture Diagram Card representation
        curY += 20f
        canvas.drawRoundRect(RectF(40f, curY, (PAGE_WIDTH - 40).toFloat(), curY + 160f), 8f, 8f, Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(RectF(40f, curY, (PAGE_WIDTH - 40).toFloat(), curY + 160f), 8f, 8f, Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })

        // Diagram boxes
        val boxPaint = Paint().apply { color = Color.rgb(37, 99, 235); style = Paint.Style.FILL }
        val boxTextPaint = Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }

        val b1 = RectF(60f, curY + 30f, 160f, curY + 80f)
        canvas.drawRoundRect(b1, 6f, 6f, boxPaint)
        canvas.drawText("Client Apps / SDK", b1.centerX(), b1.centerY() + 4f, boxTextPaint)

        val b2 = RectF(210f, curY + 30f, 330f, curY + 80f)
        canvas.drawRoundRect(b2, 6f, 6f, Paint().apply { color = Color.rgb(79, 70, 229) })
        canvas.drawText("API Gateway & WAF", b2.centerX(), b2.centerY() + 4f, boxTextPaint)

        val b3 = RectF(380f, curY + 30f, 520f, curY + 80f)
        canvas.drawRoundRect(b3, 6f, 6f, Paint().apply { color = Color.rgb(16, 185, 129) })
        canvas.drawText("Distributed Cluster", b3.centerX(), b3.centerY() + 4f, boxTextPaint)

        canvas.drawLine(160f, curY + 55f, 210f, curY + 55f, Paint().apply { color = Color.GRAY; strokeWidth = 2f })
        canvas.drawLine(330f, curY + 55f, 380f, curY + 55f, Paint().apply { color = Color.GRAY; strokeWidth = 2f })

        canvas.drawText("Figure 1.1: Multi-tiered edge ingestion and compute cluster topology", 60f, curY + 140f, Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            textSkewX = -0.25f
        })

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Cloud Architecture Technical Report • Page 1 of 2", 40f, 800f, Paint().apply { color = Color.GRAY; textSize = 10f })
        doc.finishPage(page1)

        // --- PAGE 2 ---
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = doc.startPage(pageInfo2)
        canvas = page2.canvas

        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 12f), Paint().apply { color = Color.rgb(16, 185, 129) })
        canvas.drawText("2. PERFORMANCE & LATENCY METRICS", 40f, 50f, headerPaint)
        canvas.drawLine(40f, 65f, (PAGE_WIDTH - 40).toFloat(), 65f, linePaint)

        // Table
        val tableTop = 90f
        val colW = (PAGE_WIDTH - 80) / 4f
        canvas.drawRect(RectF(40f, tableTop, (PAGE_WIDTH - 40).toFloat(), tableTop + 30f), Paint().apply { color = Color.rgb(241, 245, 249) })
        val tableHdrPaint = Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10f; isFakeBoldText = true }

        canvas.drawText("Service Tier", 50f, tableTop + 20f, tableHdrPaint)
        canvas.drawText("p50 Latency", 50f + colW, tableTop + 20f, tableHdrPaint)
        canvas.drawText("p99 Latency", 50f + colW * 2, tableTop + 20f, tableHdrPaint)
        canvas.drawText("Throughput", 50f + colW * 3, tableTop + 20f, tableHdrPaint)

        val rows = listOf(
            listOf("Edge Ingress", "4 ms", "12 ms", "45,000 req/s"),
            listOf("Auth Service", "8 ms", "21 ms", "38,000 req/s"),
            listOf("Document Storage", "15 ms", "39 ms", "19,500 req/s"),
            listOf("Search Indexer", "22 ms", "58 ms", "12,000 req/s")
        )
        var rowY = tableTop + 55f
        for (r in rows) {
            canvas.drawText(r[0], 50f, rowY, bodyPaint)
            canvas.drawText(r[1], 50f + colW, rowY, bodyPaint)
            canvas.drawText(r[2], 50f + colW * 2, rowY, bodyPaint)
            canvas.drawText(r[3], 50f + colW * 3, rowY, bodyPaint)
            canvas.drawLine(40f, rowY + 10f, (PAGE_WIDTH - 40).toFloat(), rowY + 10f, linePaint)
            rowY += 30f
        }

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Cloud Architecture Technical Report • Page 2 of 2", 40f, 800f, Paint().apply { color = Color.GRAY; textSize = 10f })
        doc.finishPage(page2)

        FileOutputStream(targetFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()
    }

    private fun generateInvoice(targetFile: File) {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawRect(RectF(0f, 0f, PAGE_WIDTH.toFloat(), 14f), Paint().apply { color = Color.rgb(79, 70, 229) })

        canvas.drawText("STUDIO LUMINA", 40f, 55f, Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 18f
            isFakeBoldText = true
        })
        canvas.drawText("Design & Software Consulting", 40f, 72f, Paint().apply { color = Color.GRAY; textSize = 10f })

        canvas.drawText("INVOICE", (PAGE_WIDTH - 150).toFloat(), 55f, titlePaint)
        canvas.drawText("#INV-2026-0842", (PAGE_WIDTH - 150).toFloat(), 75f, Paint().apply { color = Color.rgb(100, 116, 139); textSize = 11f })

        canvas.drawLine(40f, 95f, (PAGE_WIDTH - 40).toFloat(), 95f, linePaint)

        // Billed to
        canvas.drawText("BILLED TO:", 40f, 125f, headerPaint)
        canvas.drawText("Acme Corporation Worldwide", 40f, 142f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 11f; isFakeBoldText = true })
        canvas.drawText("742 Evergreen Terrace, Suite 400", 40f, 158f, bodyPaint)
        canvas.drawText("finance@acmecorp.example.com", 40f, 172f, bodyPaint)

        // Dates
        canvas.drawText("INVOICE DATE:", (PAGE_WIDTH - 180).toFloat(), 125f, headerPaint)
        canvas.drawText("October 12, 2026", (PAGE_WIDTH - 180).toFloat(), 142f, bodyPaint)
        canvas.drawText("PAYMENT DUE:", (PAGE_WIDTH - 180).toFloat(), 160f, headerPaint)
        canvas.drawText("Net 30 Days", (PAGE_WIDTH - 180).toFloat(), 175f, bodyPaint)

        // Table
        val tTop = 210f
        canvas.drawRect(RectF(40f, tTop, (PAGE_WIDTH - 40).toFloat(), tTop + 25f), Paint().apply { color = Color.rgb(241, 245, 249) })
        canvas.drawText("Description", 50f, tTop + 17f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10f; isFakeBoldText = true })
        canvas.drawText("Hours / Qty", 320f, tTop + 17f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10f; isFakeBoldText = true })
        canvas.drawText("Rate", 410f, tTop + 17f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10f; isFakeBoldText = true })
        canvas.drawText("Amount", 480f, tTop + 17f, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 10f; isFakeBoldText = true })

        val items = listOf(
            Triple("UI/UX Design System & Prototype", "40 hrs", "$150/hr"),
            Triple("Jetpack Compose Native Development", "65 hrs", "$175/hr"),
            Triple("PDF Engine & Annotation Suite", "35 hrs", "$175/hr"),
            Triple("Security Auditing & Test Suite", "15 hrs", "$160/hr")
        )
        val amounts = listOf("$6,000.00", "$11,375.00", "$6,125.00", "$2,400.00")

        var curY = tTop + 45f
        for (i in items.indices) {
            canvas.drawText(items[i].first, 50f, curY, bodyPaint)
            canvas.drawText(items[i].second, 320f, curY, bodyPaint)
            canvas.drawText(items[i].third, 410f, curY, bodyPaint)
            canvas.drawText(amounts[i], 480f, curY, bodyPaint)
            canvas.drawLine(40f, curY + 12f, (PAGE_WIDTH - 40).toFloat(), curY + 12f, linePaint)
            curY += 32f
        }

        // Totals Box
        curY += 20f
        canvas.drawText("Subtotal:", 380f, curY, bodyPaint)
        canvas.drawText("$25,900.00", 480f, curY, bodyPaint)
        curY += 20f
        canvas.drawText("Sales Tax (0%):", 380f, curY, bodyPaint)
        canvas.drawText("$0.00", 480f, curY, bodyPaint)
        curY += 25f
        canvas.drawLine(380f, curY - 10f, (PAGE_WIDTH - 40).toFloat(), curY - 10f, linePaint)

        canvas.drawText("TOTAL BALANCE:", 340f, curY, Paint().apply { color = Color.rgb(15, 23, 42); textSize = 12f; isFakeBoldText = true })
        canvas.drawText("$25,900.00", 470f, curY, Paint().apply { color = Color.rgb(37, 99, 235); textSize = 14f; isFakeBoldText = true })

        // Payment info callout
        canvas.drawRoundRect(RectF(40f, 620f, (PAGE_WIDTH - 40).toFloat(), 720f), 8f, 8f, Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
        })
        canvas.drawText("PAYMENT METHOD: Direct Wire Transfer / ACH", 55f, 650f, Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 10f
            isFakeBoldText = true
        })
        canvas.drawText("Bank: Silicon Valley Commercial Bank • Routing: 121000358 • Account: 9845-2018-9102", 55f, 675f, Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 9f
        })
        canvas.drawText("Thank you for your business! Please apply PAID stamp upon settlement.", 55f, 695f, Paint().apply {
            color = Color.rgb(21, 128, 61)
            textSize = 9f
            textSkewX = -0.25f
        })

        // Footer
        canvas.drawLine(40f, 780f, (PAGE_WIDTH - 40).toFloat(), 780f, linePaint)
        canvas.drawText("Studio Lumina Consulting • contact@studiolumina.example.com", 40f, 800f, Paint().apply { color = Color.GRAY; textSize = 10f })
        doc.finishPage(page)

        FileOutputStream(targetFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()
    }
}
