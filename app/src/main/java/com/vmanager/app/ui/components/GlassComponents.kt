package com.vmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vmanager.app.ui.theme.AccentCyan
import com.vmanager.app.ui.theme.BorderGlass
import com.vmanager.app.ui.theme.LocalGlassEnabled
import com.vmanager.app.ui.theme.LocalHazeState
import com.vmanager.app.ui.theme.SurfaceGlass
import com.vmanager.app.ui.theme.SurfaceGlassStrong
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassEnabled = LocalGlassEnabled.current
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(16.dp)

    val cardModifier = modifier
        .clip(shape)
        .then(
            if (glassEnabled) {
                Modifier
                    .hazeEffect(state = hazeState) {
                        style = HazeMaterials.ultraThin()
                    }
                    .background(SurfaceGlass)
                    .border(1.dp, BorderGlass, shape)
            } else {
                Modifier
                    .background(SurfaceGlassStrong, shape)
                    .border(1.dp, BorderGlass.copy(alpha = 0.15f), shape)
            }
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(16.dp)

    Column(modifier = cardModifier, content = content)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassEnabled = LocalGlassEnabled.current
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (glassEnabled) {
                    Modifier
                        .hazeEffect(state = hazeState) {
                            style = HazeMaterials.thin()
                        }
                        .background(SurfaceGlass)
                        .border(0.5.dp, BorderGlass, shape)
                } else {
                    Modifier.background(SurfaceGlassStrong, shape)
                }
            )
    ) {
        content()
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AccentCyan,
            checkedTrackColor = AccentCyan.copy(alpha = 0.4f),
            uncheckedThumbColor = Color.Gray,
            uncheckedTrackColor = Color.DarkGray
        )
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = AccentCyan,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
