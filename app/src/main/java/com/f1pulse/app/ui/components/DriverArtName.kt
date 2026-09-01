package com.f1pulse.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.f1pulse.app.ui.theme.Formula1Family
import com.f1pulse.app.ui.theme.NorthwellCleanAltFamily

/**
 * Driver name treatment used by the detail hero. The full name is always shown
 * — no ellipsis, no truncation. Long surnames (e.g. "ANTONELLI") may overlap
 * the headshot photo, which is intentional per the F1 broadcast style.
 */
@Composable
fun DriverArtName(
    givenName: String,
    familyName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = givenName,
            fontFamily = NorthwellCleanAltFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            color = Color.White,
            lineHeight = 42.sp,
            maxLines = 1,
        )
        Text(
            text = familyName.uppercase(),
            fontFamily = Formula1Family,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            lineHeight = 36.sp,
            maxLines = 1,
        )
    }
}
