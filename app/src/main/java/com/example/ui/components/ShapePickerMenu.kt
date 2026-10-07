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
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ShapeType

@Composable
fun ShapePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (ShapeType, strokeColorHex: Long, strokeWidth: Float, hasFill: Boolean) -> Unit
) {
    var selectedShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var strokeColorHex by remember { mutableLongStateOf(0xFF2563EB) }
    var strokeWidth by remember { mutableFloatStateOf(4f) }
    var hasFill by remember { mutableStateOf(false) }

    val shapeIcons = listOf<Triple<ShapeType, ImageVector, String>>(
        Triple(ShapeType.RECTANGLE, Icons.Default.CropLandscape, "Rectangle"),
        Triple(ShapeType.CIRCLE, Icons.Default.RadioButtonUnchecked, "Circle"),
        Triple(ShapeType.ARROW, Icons.Default.TrendingFlat, "Arrow"),
        Triple(ShapeType.LINE, Icons.Default.HorizontalRule, "Line"),
        Triple(ShapeType.REDACTION, Icons.Default.Security, "Redact Box")
    )

    val colorOptions = listOf(
        0xFF2563EB to "Blue",
        0xFFDC2626 to "Red",
        0xFF16A34A to "Green",
        0xFFD97706 to "Amber",
        0xFF0F172A to "Black"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("shape_picker_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Shapes & Markup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shapes Grid/Chips
                Text("Select Shape:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shapeIcons.forEach { (type, icon, label) ->
                        val isSelected = selectedShape == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedShape = type }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (selectedShape == ShapeType.REDACTION) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Redaction box creates an opaque blackout shield to securely obscure sensitive clauses, figures, or identities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Stroke Color
                    Text("Stroke Color:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorOptions.forEach { (colorHex, _) ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = if (strokeColorHex == colorHex) 2.5.dp else 1.dp,
                                        color = if (strokeColorHex == colorHex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { strokeColorHex = colorHex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stroke width
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thickness: ${strokeWidth.toInt()}px", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(90.dp))
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..14f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedShape == ShapeType.RECTANGLE || selectedShape == ShapeType.CIRCLE) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = hasFill,
                                onClick = { hasFill = !hasFill },
                                label = { Text("Semi-Transparent Color Fill") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val finalColor = if (selectedShape == ShapeType.REDACTION) 0xFF000000 else strokeColorHex
                        onConfirm(selectedShape, finalColor, strokeWidth, hasFill)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_shape_button")
                ) {
                    Text("Insert Shape Onto Page")
                }
            }
        }
    }
}
