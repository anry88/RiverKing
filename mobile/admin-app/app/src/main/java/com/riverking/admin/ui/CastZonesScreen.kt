package com.riverking.admin.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.riverking.admin.network.AdminApiClient
import com.riverking.admin.network.CastZoneDTO
import com.riverking.admin.network.CastZoneLocationDTO
import com.riverking.admin.network.CastZonePointDTO
import com.riverking.admin.ui.theme.riverBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.abs
import kotlin.math.hypot

private val DefaultCastZonePoints = listOf(
    CastZonePointDTO(0.08, 0.62),
    CastZonePointDTO(0.48, 0.62),
    CastZonePointDTO(0.48, 0.94),
    CastZonePointDTO(0.08, 0.94),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastZonesScreen(apiClient: AdminApiClient, onBack: () -> Unit) {
    var locations by remember { mutableStateOf<List<CastZoneLocationDTO>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<CastZoneLocationDTO?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun reload() {
        scope.launch {
            loading = true
            try {
                locations = apiClient.getCastZones()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Load failed: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cast Zones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .riverBackdrop()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                item { Text("Loading...") }
            }
            items(locations) { location ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { editing = location },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(location.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${kindLabel(location.kind)}${location.eventId?.let { " #$it" } ?: ""}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = location.castZone?.points?.let { "${it.size} points" } ?: "Fallback zone",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (location.castZone != null) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    editing?.let { location ->
        CastZoneEditorDialog(
            apiClient = apiClient,
            location = location,
            onDismiss = { editing = null },
            onSaved = {
                editing = null
                reload()
            },
            onError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
        )
    }
}

@Composable
private fun CastZoneEditorDialog(
    apiClient: AdminApiClient,
    location: CastZoneLocationDTO,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val imageUrl = remember(location.imageUrl, apiClient.baseUrl) { apiClient.publicUrl(location.imageUrl) }
    val image = rememberRemoteBitmap(imageUrl)
    var points by remember(location.id, location.castZone) {
        mutableStateOf(location.castZone?.points ?: emptyList())
    }
    var saving by remember(location.id) { mutableStateOf(false) }
    var error by remember(location.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.92f),
        title = { Text(location.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${kindLabel(location.kind)} location", color = MaterialTheme.colorScheme.onSurfaceVariant)
                CastZoneCanvas(
                    image = image,
                    points = points,
                    onPointsChange = {
                        points = it
                        error = null
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { points = DefaultCastZonePoints }) {
                        Text("Default")
                    }
                    Button(onClick = { if (points.isNotEmpty()) points = points.dropLast(1) }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                    TextButton(onClick = { points = emptyList() }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                Text(
                    "Tap or drag on the image to add/move points. The last point closes back to the first point.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val zone = when {
                        points.isEmpty() -> null
                        points.size < 3 -> {
                            error = "Use at least 3 points or clear the zone."
                            return@TextButton
                        }
                        polygonArea(points) < 0.0004 -> {
                            error = "Zone is too small."
                            return@TextButton
                        }
                        else -> CastZoneDTO(points)
                    }
                    saving = true
                    scope.launch {
                        try {
                            apiClient.updateCastZone(location.id, zone)
                            onSaved()
                        } catch (e: Exception) {
                            onError("Save failed: ${e.message}")
                            saving = false
                        }
                    }
                },
            ) { Text(if (saving) "Saving..." else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun rememberRemoteBitmap(url: String?): Bitmap? {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        if (url.isNullOrBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun CastZoneCanvas(
    image: Bitmap?,
    points: List<CastZonePointDTO>,
    onPointsChange: (List<CastZonePointDTO>) -> Unit,
) {
    var dragIndex by remember(points) { mutableStateOf<Int?>(null) }
    val aspect = image?.let { it.width.toFloat() / it.height.coerceAtLeast(1).toFloat() } ?: 1.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(points, image) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val height = size.height.toFloat().coerceAtLeast(1f)
                        val nearest = nearestPointIndex(points, offset, width, height, maxDistance = 32.dp.toPx())
                        if (nearest == null) {
                            val nextPoint = offsetToPoint(offset, width, height)
                            val updated = points + nextPoint
                            onPointsChange(updated)
                            dragIndex = updated.lastIndex
                        } else {
                            dragIndex = nearest
                        }
                    },
                    onDrag = { change, _ ->
                        val index = dragIndex ?: return@detectDragGestures
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val height = size.height.toFloat().coerceAtLeast(1f)
                        onPointsChange(points.toMutableList().also {
                            if (index in it.indices) {
                                it[index] = offsetToPoint(change.position, width, height)
                            }
                        })
                        change.consume()
                    },
                    onDragEnd = { dragIndex = null },
                    onDragCancel = { dragIndex = null },
                )
            },
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFF18222E))
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val offset = pointToOffset(point, size.width, size.height)
                if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            if (points.size >= 3) {
                path.close()
                drawPath(path, Color(0x4400E5D0))
                drawPath(path, Color(0xFF78E0D4), style = Stroke(width = 3.dp.toPx()))
            } else if (points.size >= 2) {
                drawPath(path, Color(0xFF78E0D4), style = Stroke(width = 3.dp.toPx()))
            }
            points.forEachIndexed { index, point ->
                val offset = pointToOffset(point, size.width, size.height)
                drawCircle(
                    color = if (dragIndex == index) Color(0xFFFFD166) else Color(0xFFFFFFFF),
                    radius = 7.dp.toPx(),
                    center = offset,
                )
                drawCircle(
                    color = Color(0xFF0B1B22),
                    radius = 3.dp.toPx(),
                    center = offset,
                )
            }
        }
    }
}

private fun kindLabel(kind: String): String =
    if (kind == "event") "Event" else "Regular"

private fun offsetToPoint(offset: Offset, width: Float, height: Float): CastZonePointDTO =
    CastZonePointDTO(
        x = (offset.x / width).coerceIn(0f, 1f).toDouble(),
        y = (offset.y / height).coerceIn(0f, 1f).toDouble(),
    )

private fun pointToOffset(point: CastZonePointDTO, width: Float, height: Float): Offset =
    Offset(
        x = (point.x.toFloat().coerceIn(0f, 1f) * width),
        y = (point.y.toFloat().coerceIn(0f, 1f) * height),
    )

private fun nearestPointIndex(
    points: List<CastZonePointDTO>,
    offset: Offset,
    width: Float,
    height: Float,
    maxDistance: Float,
): Int? {
    var nearestIndex: Int? = null
    var nearestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val pointOffset = pointToOffset(point, width, height)
        val distance = hypot(pointOffset.x - offset.x, pointOffset.y - offset.y)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearestIndex = index
        }
    }
    return nearestIndex.takeIf { nearestDistance <= maxDistance }
}

private fun polygonArea(points: List<CastZonePointDTO>): Double {
    var sum = 0.0
    points.forEachIndexed { index, point ->
        val next = points[(index + 1) % points.size]
        sum += point.x * next.y - next.x * point.y
    }
    return abs(sum) / 2.0
}
