package com.neoncartel.drugwars.ui

import androidx.compose.ui.layout.ContentScale
import org.junit.Assert.assertEquals
import org.junit.Test

class CityBackdropLayoutTest {
    @Test
    fun anchorageBannerUsesFullWidthScalingSoLeftEdgeStaysVisibleOnSmallPhones() {
        assertEquals(ContentScale.FillWidth, CityBackdropLayout.AnchorageBannerContentScale)
        assertEquals(
            0f,
            CityBackdropLayout.anchorageHorizontalCropPx(
                containerWidthPx = 320f,
                containerHeightPx = 132f,
                imageWidthPx = 1733f,
                imageHeightPx = 658f,
            ),
        )
    }
}
