package com.desmond.feedpack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desmond.core.model.DesmondPalette
import com.desmond.plugins.launcher.WidgetRenderState
import com.desmond.plugins.launcher.WidgetRenderer

class WeatherWidgetRenderer(private val service: WeatherService) : WidgetRenderer {
    override val id: String = "com.desmond.feedpack.WeatherWidgetRenderer"
    @Composable
    override fun Render(state: WidgetRenderState, onAction: (String, String) -> Unit, expanded: Boolean) {
        val s = service.currentState()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(DesmondPalette.AMBER)).border(1.5.dp, Color(DesmondPalette.INK), RoundedCornerShape(24.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(s?.title ?: "Weather", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(DesmondPalette.INK))
            Text((s?.status ?: "Loading").uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(DesmondPalette.INK), letterSpacing = 1.2.sp)
            HorizontalDivider(color = Color(DesmondPalette.INK), thickness = 1.5.dp)
            (s?.items ?: emptyList()).forEach { Text("• $it", color = Color(DesmondPalette.INK), style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = { onAction("refresh","") }, shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(DesmondPalette.INK)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(DesmondPalette.INK))) { Text("REFRESH") }
        }
    }
}
