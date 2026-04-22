package com.riverking.admin.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RiverDeepNight = Color(0xFF071319)
val RiverAbyss = Color(0xFF0B1D24)
val RiverCurrent = Color(0xFF11333B)
val RiverLagoon = Color(0xFF18454D)
val RiverPanel = Color(0xE6132A33)
val RiverPanelRaised = Color(0xF0183440)
val RiverPanelMuted = Color(0xCC1A3A43)
val RiverPanelSoft = Color(0xB31A3941)
val RiverMist = Color(0xFFF0F5F1)
val RiverFog = Color(0xFFA8C0BF)
val RiverFoam = Color(0xFF74D6C8)
val RiverTide = Color(0xFF58B7C8)
val RiverMoss = Color(0xFF8CC58E)
val RiverAmber = Color(0xFFD8B16D)
val RiverCoral = Color(0xFFDC8F67)
val RiverSlate = Color(0xFF718A94)
val RiverDanger = Color(0xFFE9766F)
val RiverOutline = Color(0x405F7E85)

val RiverBackdropBrush = Brush.verticalGradient(
    colors = listOf(
        RiverDeepNight,
        RiverAbyss,
        RiverCurrent,
        RiverLagoon,
        RiverDeepNight,
    )
)

val RiverSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val RiverDialogShape = RoundedCornerShape(30.dp)

private val RiverDarkScheme = darkColorScheme(
    primary = RiverFoam,
    onPrimary = RiverDeepNight,
    secondary = RiverAmber,
    onSecondary = RiverDeepNight,
    tertiary = RiverMoss,
    onTertiary = RiverDeepNight,
    background = RiverDeepNight,
    onBackground = RiverMist,
    surface = RiverPanel,
    onSurface = RiverMist,
    surfaceVariant = RiverPanelMuted,
    onSurfaceVariant = RiverFog,
    outline = RiverOutline,
    error = RiverDanger,
    onError = RiverDeepNight,
)

private val RiverTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val RiverShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

fun Modifier.riverBackdrop(): Modifier = drawWithCache {
    val coolGlow = Brush.radialGradient(
        colors = listOf(RiverFoam.copy(alpha = 0.18f), Color.Transparent),
        center = Offset(size.width * 0.18f, size.height * 0.10f),
        radius = size.maxDimension * 0.92f,
    )
    val amberGlow = Brush.radialGradient(
        colors = listOf(RiverAmber.copy(alpha = 0.12f), Color.Transparent),
        center = Offset(size.width * 0.84f, size.height * 0.16f),
        radius = size.maxDimension * 0.76f,
    )
    val mossGlow = Brush.radialGradient(
        colors = listOf(RiverMoss.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(size.width * 0.52f, size.height * 1.02f),
        radius = size.maxDimension * 0.84f,
    )
    onDrawBehind {
        drawRect(brush = RiverBackdropBrush)
        drawRect(brush = coolGlow)
        drawRect(brush = amberGlow)
        drawRect(brush = mossGlow)
    }
}

@Composable
fun RiverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RiverDarkScheme,
        typography = RiverTypography,
        shapes = RiverShapes,
        content = content,
    )
}

@Composable
fun riverTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    focusedContainerColor = RiverPanelSoft.copy(alpha = 0.70f),
    unfocusedContainerColor = RiverPanelSoft.copy(alpha = 0.46f),
    disabledContainerColor = RiverPanelSoft.copy(alpha = 0.26f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = RiverOutline.copy(alpha = 0.75f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun riverPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = RiverSlate.copy(alpha = 0.42f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun riverSecondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondary,
    contentColor = MaterialTheme.colorScheme.onSecondary,
    disabledContainerColor = RiverSlate.copy(alpha = 0.42f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun riverOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    containerColor = RiverPanelSoft.copy(alpha = 0.78f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    disabledContainerColor = RiverPanelSoft.copy(alpha = 0.32f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

fun riverOutlineBorder() = BorderStroke(1.dp, RiverOutline.copy(alpha = 0.82f))
