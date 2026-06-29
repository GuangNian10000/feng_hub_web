package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainControlPanel(
    activeFragment: String?,
    inspectionTab: String,
    onClose: () -> Unit,
    onTabChange: (String) -> Unit,
    onAddClickAction: (() -> Unit)?,
    addActionText: String,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(modifier = modifier.fillMaxWidth().fillMaxHeight(), bgColor = Color(0xD9121212)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeFragment == "导览管理") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "《 ",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onClose() }
                                .padding(4.dp)
                        )
                        Text(
                            text = "导览点",
                            color = if (inspectionTab == "导览点") Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (inspectionTab == "导览点") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTabChange("导览点") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(if (inspectionTab == "导览点") Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "路线",
                            color = if (inspectionTab == "路线") Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (inspectionTab == "路线") FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTabChange("路线") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(if (inspectionTab == "路线") Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier)
                        )
                    }
                } else {
                    Text(
                        text = "《 ${activeFragment ?: ""}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClose() }
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onAddClickAction != null) {
                    Button(
                        onClick = { onAddClickAction.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp).padding(end = 12.dp)
                    ) {
                        Text(addActionText, fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
        }
    }
}
