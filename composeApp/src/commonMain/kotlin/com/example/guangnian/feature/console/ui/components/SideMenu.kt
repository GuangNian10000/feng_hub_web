package com.example.guangnian.feature.console.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SideMenu(
    menus: List<String>,
    activeFragment: String?,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(modifier = modifier.width(130.dp).heightIn(max = 240.dp), bgColor = Color(0xAA000000)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(4.dp)) {
            items(menus) { menu ->
                MenuListItem(
                    text = menu,
                    isSelected = activeFragment == menu,
                    onClick = { onMenuClick(menu) }
                )
            }
        }
    }
}

@Composable
fun MenuListItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0x44FFFFFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text, 
            color = if (isSelected) Color.White else Color.LightGray, 
            fontSize = 13.sp, 
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
