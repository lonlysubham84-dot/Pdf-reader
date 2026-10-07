package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.TextAnnotation

@Composable
fun TextAnnotationDialog(
    initialAnnotation: TextAnnotation? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        fontSizeSp: Float,
        textColorHex: Long,
        bgColorHex: Long,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        fontFamilyName: String
    ) -> Unit
) {
    var textValue by remember { mutableStateOf(initialAnnotation?.text ?: "") }
    var fontSizeSp by remember { mutableFloatStateOf(initialAnnotation?.fontSizeSp ?: 16f) }
    var textColorHex by remember { mutableLongStateOf(initialAnnotation?.textColorHex ?: 0xFF0F172A) }
    var bgColorHex by remember { mutableLongStateOf(initialAnnotation?.bgColorHex ?: 0L) }
    var isBold by remember { mutableStateOf(initialAnnotation?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(initialAnnotation?.isItalic ?: false) }
    var isUnderline by remember { mutableStateOf(initialAnnotation?.isUnderline ?: false) }
    var fontFamilyName by remember { mutableStateOf(initialAnnotation?.fontFamilyName ?: "Default") }

    val fontFamilies = listOf("Default", "Serif", "Mono")

    val textColors = listOf(
        0xFF0F172A to "Obsidian",
        0xFF2563EB to "Blue",
        0xFFDC2626 to "Crimson",
        0xFF16A34A to "Emerald",
        0xFF7C3AED to "Purple",
        0xFFD97706 to "Amber"
    )

    val bgHighlights = listOf(
        0L to "None",
        0xFFFEF08A to "Yellow",
        0xFFBBF7D0 to "Green",
        0xFFBFDBFE to "Blue",
        0xFFFECACA to "Pink"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("text_annotation_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialAnnotation != null) "Edit Text Block" else "Add Text Overlay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text Field Input
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = { Text("Enter custom text note, date, or title...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_annotation_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Box
                Text("Preview:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val previewFontFamily = when (fontFamilyName.lowercase()) {
                        "serif" -> FontFamily.Serif
                        "mono" -> FontFamily.Monospace
                        else -> FontFamily.Default
                    }

                    Box(
                        modifier = if (bgColorHex != 0L) {
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(bgColorHex))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        } else Modifier
                    ) {
                        Text(
                            text = if (textValue.isNotBlank()) textValue else "Sample Text Preview",
                            color = Color(textColorHex),
                            fontSize = fontSizeSp.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                            fontFamily = previewFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Typography & Style controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Font Family chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fontFamilies.forEach { font ->
                            FilterChip(
                                selected = fontFamilyName == font,
                                onClick = { fontFamilyName = font },
                                label = { Text(font, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Bold / Italic / Underline
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconToggleButton(checked = isBold, onCheckedChange = { isBold = it }) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = if (isBold) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        IconToggleButton(checked = isItalic, onCheckedChange = { isItalic = it }) {
                            Icon(
                                imageVector = Icons.Default.FormatItalic,
                                contentDescription = "Italic",
                                tint = if (isItalic) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        IconToggleButton(checked = isUnderline, onCheckedChange = { isUnderline = it }) {
                            Icon(
                                imageVector = Icons.Default.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (isUnderline) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${fontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = fontSizeSp,
                        onValueChange = { fontSizeSp = it },
                        valueRange = 10f..40f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Text Color Palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Color:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    textColors.forEach { (colorVal, _) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (textColorHex == colorVal) 2.5.dp else 1.dp,
                                    color = if (textColorHex == colorVal) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { textColorHex = colorVal }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Color Palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Highlight:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    bgHighlights.forEach { (bgVal, label) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (bgVal == 0L) Color.Transparent else Color(bgVal))
                                .border(
                                    width = if (bgColorHex == bgVal) 2.dp else 1.dp,
                                    color = if (bgColorHex == bgVal) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { bgColorHex = bgVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bgVal == 0L) {
                                Text("ø", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (textValue.isNotBlank()) {
                            onConfirm(textValue, fontSizeSp, textColorHex, bgColorHex, isBold, isItalic, isUnderline, fontFamilyName)
                        }
                    },
                    enabled = textValue.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_text_button")
                ) {
                    Text(if (initialAnnotation != null) "Update Text" else "Insert Text Onto PDF")
                }
            }
        }
    }
}
