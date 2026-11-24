package com.example.guangnian

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    device = "spec:width=896dp,height=414dp,orientation=landscape",
    showSystemUi = true,
    showBackground = true
)
@Composable
fun AppLandscapePreview() {
    MaterialTheme(colors = darkColors()) {
        TestScreen()
    }
}
