package jp.kira.sdwebuiremote.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Composable
fun ImageCanvas(
    baseImage: Bitmap?,
    modifier: Modifier = Modifier,
    brushSize: Float,
    paths: SnapshotStateList<Path>,
    onMaskChange: (Bitmap?) -> Unit
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val imageSize = remember { mutableStateOf(IntSize.Zero) }

    val drawModifier = modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPath = Path().also { it.moveTo(offset.x, offset.y) }
                },
                onDrag = { change, _ ->
                    currentPath?.lineTo(change.position.x, change.position.y)
                    // Forcing recomposition by creating a new list
                    paths.add(currentPath!!)
                    paths.remove(currentPath!!)
                },
                onDragEnd = {
                    currentPath?.let { paths.add(it) }
                    currentPath = null
                }
            )
        }
        .onSizeChanged { size ->
            imageSize.value = size
        }

    LaunchedEffect(paths.size, imageSize.value, brushSize) {
        if (imageSize.value.width > 0 && imageSize.value.height > 0) {
            if (paths.isEmpty()) {
                onMaskChange(null)
                return@LaunchedEffect
            }
            val newBitmap = Bitmap.createBitmap(imageSize.value.width, imageSize.value.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(newBitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                strokeWidth = brushSize
                style = android.graphics.Paint.Style.STROKE
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeCap = android.graphics.Paint.Cap.ROUND
            }
            paths.forEach { path ->
                canvas.drawPath(path.asAndroidPath(), paint)
            }
            onMaskChange(newBitmap)
        }
    }

    Box(modifier = modifier) {
        val image = baseImage
        Canvas(modifier = drawModifier.fillMaxSize()) {
            if (image != null) {
                drawImage(image.asImageBitmap())
            }
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.5f),
                    style = Stroke(width = brushSize, join = StrokeJoin.Round, cap = StrokeCap.Round)
                )
            }
            currentPath?.let {
                drawPath(
                    path = it,
                    color = Color.Black.copy(alpha = 0.5f),
                    style = Stroke(width = brushSize, join = StrokeJoin.Round, cap = StrokeCap.Round)
                )
            }
        }
    }
}
