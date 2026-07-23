package com.example.guangnian.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

object ModernDialogColors {
    val Accent = Color(0xFF1769E0)
    val Canvas = Color(0xFFF3F5F7)
    val Surface = Color(0xFFFFFFFF)
    val PrimaryText = Color(0xFF171A1F)
    val SecondaryText = Color(0xFF667085)
    val Border = Color(0xFFD6DAE1)
    val Divider = Color(0xFFE6E8EC)
}

@Composable
fun ModernFormDialog(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModernDialogScaffold(
        title = title,
        onDismiss = onDismiss,
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = ModernDialogColors.SecondaryText)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ModernDialogColors.Accent,
                    contentColor = Color.White,
                    disabledBackgroundColor = ModernDialogColors.Accent.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.75f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Medium)
            }
        },
        content = content
    )
}

@Composable
fun ModernDialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: Dp = 920.dp,
    maxHeight: Dp = 680.dp,
    titleFontSize: TextUnit = 20.sp,
    showFooter: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(18.dp),
    headerHeight: Dp = 48.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(8.dp),
                color = ModernDialogColors.Surface,
                elevation = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight)
                            .padding(start = 24.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 16.dp)
                                .background(ModernDialogColors.Accent, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = title,
                            color = ModernDialogColors.PrimaryText,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = ModernDialogColors.SecondaryText
                            )
                        }
                    }

                    Divider(color = ModernDialogColors.Divider)

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .background(ModernDialogColors.Canvas)
                            .verticalScroll(rememberScrollState())
                            .padding(contentPadding),
                        verticalArrangement = verticalArrangement,
                        content = content
                    )

                    if (showFooter) {
                        Divider(color = ModernDialogColors.Divider)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) { actions() }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernDialogSection(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = ModernDialogColors.PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(10.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = ModernDialogColors.Divider
        )
    }
}

@Composable
fun modernOutlinedTextFieldColors(): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
    textColor = ModernDialogColors.PrimaryText,
    cursorColor = ModernDialogColors.Accent,
    focusedBorderColor = ModernDialogColors.Accent,
    unfocusedBorderColor = ModernDialogColors.Border,
    focusedLabelColor = ModernDialogColors.Accent,
    unfocusedLabelColor = ModernDialogColors.SecondaryText,
    backgroundColor = ModernDialogColors.Surface
)

@Composable
fun ModernToggleOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .background(ModernDialogColors.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, ModernDialogColors.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ModernDialogColors.PrimaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ModernDialogColors.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFADB3BD)
            )
        )
    }
}
