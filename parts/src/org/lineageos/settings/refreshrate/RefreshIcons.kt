/* Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.refreshrate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object RefreshIcons {
    // Display with a clock: standard timed refresh, distinct from the fast-rate gauge.
    val standard =
        ImageVector.Builder("standard_refresh", 24.dp, 24.dp, 24f, 24f)
            .apply {
                listOf(
                        "M11 17H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5",
                        "M7 21h4M9 17v4",
                        "M13 16a5 5 0 1 0 10 0a5 5 0 1 0-10 0",
                        "M18 13v3l2 1",
                    )
                    .forEach {
                        addPath(
                            PathParser().parsePathString(it).toNodes(),
                            stroke = SolidColor(Color.Black),
                            strokeLineWidth = 2f,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round,
                        )
                    }
            }
            .build()
}
