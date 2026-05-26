package com.neoncartel.drugwars.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.neoncartel.drugwars.R
import kotlin.math.roundToInt

data class MenuBoxFrameInsets(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

object MenuBoxFrameGeometry {
    const val SourceLeftPx = 72
    const val SourceTopPx = 64
    const val SourceRightPx = 72
    const val SourceBottomPx = 64

    fun destinationInsets(
        width: Float,
        height: Float,
        horizontalEdge: Float,
        verticalEdge: Float,
    ): MenuBoxFrameInsets {
        val safeWidth = width.coerceAtLeast(0f)
        val safeHeight = height.coerceAtLeast(0f)
        val leftRight = horizontalEdge.coerceAtLeast(0f).coerceAtMost(safeWidth / 2f)
        val topBottom = verticalEdge.coerceAtLeast(0f).coerceAtMost(safeHeight / 2f)
        return MenuBoxFrameInsets(
            left = leftRight,
            top = topBottom,
            right = leftRight,
            bottom = topBottom,
        )
    }
}

@Composable
fun MenuBoxFrame(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    edgeWidth: Dp = 22.dp,
    edgeHeight: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val image = ImageBitmap.imageResource(R.drawable.menu_box)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawMenuBoxFrame(
                image = image,
                edgeWidth = edgeWidth.toPx(),
                edgeHeight = edgeHeight.toPx(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content,
        )
    }
}

private fun DrawScope.drawMenuBoxFrame(
    image: ImageBitmap,
    edgeWidth: Float,
    edgeHeight: Float,
) {
    val dst = MenuBoxFrameGeometry.destinationInsets(
        width = size.width,
        height = size.height,
        horizontalEdge = edgeWidth,
        verticalEdge = edgeHeight,
    )
    val srcLeft = MenuBoxFrameGeometry.SourceLeftPx
    val srcTop = MenuBoxFrameGeometry.SourceTopPx
    val srcRight = MenuBoxFrameGeometry.SourceRightPx
    val srcBottom = MenuBoxFrameGeometry.SourceBottomPx
    val srcCenterWidth = image.width - srcLeft - srcRight
    val srcCenterHeight = image.height - srcTop - srcBottom
    val dstCenterWidth = size.width - dst.left - dst.right
    val dstCenterHeight = size.height - dst.top - dst.bottom

    drawFrameSlice(image, 0, 0, srcLeft, srcTop, 0f, 0f, dst.left, dst.top)
    drawFrameSlice(image, srcLeft, 0, srcCenterWidth, srcTop, dst.left, 0f, dstCenterWidth, dst.top)
    drawFrameSlice(image, image.width - srcRight, 0, srcRight, srcTop, size.width - dst.right, 0f, dst.right, dst.top)

    drawFrameSlice(image, 0, srcTop, srcLeft, srcCenterHeight, 0f, dst.top, dst.left, dstCenterHeight)
    drawFrameSlice(image, srcLeft, srcTop, srcCenterWidth, srcCenterHeight, dst.left, dst.top, dstCenterWidth, dstCenterHeight)
    drawFrameSlice(image, image.width - srcRight, srcTop, srcRight, srcCenterHeight, size.width - dst.right, dst.top, dst.right, dstCenterHeight)

    drawFrameSlice(image, 0, image.height - srcBottom, srcLeft, srcBottom, 0f, size.height - dst.bottom, dst.left, dst.bottom)
    drawFrameSlice(image, srcLeft, image.height - srcBottom, srcCenterWidth, srcBottom, dst.left, size.height - dst.bottom, dstCenterWidth, dst.bottom)
    drawFrameSlice(image, image.width - srcRight, image.height - srcBottom, srcRight, srcBottom, size.width - dst.right, size.height - dst.bottom, dst.right, dst.bottom)
}

private fun DrawScope.drawFrameSlice(
    image: ImageBitmap,
    srcX: Int,
    srcY: Int,
    srcWidth: Int,
    srcHeight: Int,
    dstX: Float,
    dstY: Float,
    dstWidth: Float,
    dstHeight: Float,
) {
    if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0f || dstHeight <= 0f) return

    drawImage(
        image = image,
        srcOffset = IntOffset(srcX, srcY),
        srcSize = IntSize(srcWidth, srcHeight),
        dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
        dstSize = Size(dstWidth, dstHeight).roundToIntSize(),
    )
}

private fun Size.roundToIntSize(): IntSize = IntSize(width.roundToInt(), height.roundToInt())
