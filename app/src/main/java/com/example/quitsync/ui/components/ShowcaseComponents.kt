package com.example.quitsync.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ShowcaseOverlay(
    isVisible: Boolean,
    currentStep: Int,
    targetRect: Rect?,
    text: String,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    isLastStep: Boolean
) {
    if (!isVisible) return

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Block clicks to underlying UI */ }
            }
            .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear
    ) {
        // The translucent background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.7f))

            // Draw the "hole" only if we have coordinates
            targetRect?.let { rect ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = rect.topLeft.copy(
                        x = rect.topLeft.x - 8.dp.toPx(),
                        y = rect.topLeft.y - 8.dp.toPx()
                    ),
                    size = rect.size.copy(
                        width = rect.width + 16.dp.toPx(),
                        height = rect.height + 16.dp.toPx()
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        if (targetRect == null) {
            // Loading state while waiting for screen layout
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            // Tooltip Bubble
            val tooltipY = if (targetRect.bottom > (density.run { 400.dp.toPx() })) {
                // Target is in bottom half, show tooltip above
                targetRect.top - density.run { 180.dp.toPx() }
            } else {
                // Target is in top half, show tooltip below
                targetRect.bottom + density.run { 20.dp.toPx() }
            }

            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset { IntOffset(0, tooltipY.roundToInt().coerceIn(0, (density.run { 600.dp.toPx() }).toInt())) }
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Step ${currentStep + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onNext) {
                            Text(if (isLastStep) "Got it!" else "Next")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable modifier to tag a component for the showcase.
 */
fun Modifier.showcaseTarget(
    tag: String,
    onCoordinates: (String, Rect) -> Unit
): Modifier = this.onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        onCoordinates(tag, coordinates.boundsInRoot())
    }
}
