/*
 * Lucide artwork: Copyright (c) 2026 Lucide Icons and Contributors, ISC.
 * Custom music/EQ artwork: Copyright (C) 2026 The LineageOS Project, Apache-2.0.
 * Full provenance: parts/licenses/misound-icons.md.
 */
package org.lineageos.settings.dirac

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object MiSoundIcons {
    private fun outline(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                paths.forEach { path ->
                    addPath(
                        pathData = PathParser().parsePathString(path).toNodes(),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()

    val guitar =
        outline(
            "guitar",
            "m11.9 12.1 4.514-4.514",
            "M20.1 2.3a1 1 0 0 0-1.4 0l-1.114 1.114A2 2 0 0 0 17 4.828v1.344a2 2 0 0 1-.586 1.414A2 2 0 0 1 17.828 7h1.344a2 2 0 0 0 1.414-.586L21.7 5.3a1 1 0 0 0 0-1.4z",
            "m6 16 2 2",
            "M8.23 9.85A3 3 0 0 1 11 8a5 5 0 0 1 5 5 3 3 0 0 1-1.85 2.77l-.92.38A2 2 0 0 0 12 18a4 4 0 0 1-4 4 6 6 0 0 1-6-6 4 4 0 0 1 4-4 2 2 0 0 0 1.85-1.23z",
        )
    val disc_3 =
        outline(
            "disc-3",
            "M2.0,12.0a10.0,10.0 0 1,0 20.0,0a10.0,10.0 0 1,0 -20.0,0",
            "M6 12c0-1.7.7-3.2 1.8-4.2",
            "M10.0,12.0a2.0,2.0 0 1,0 4.0,0a2.0,2.0 0 1,0 -4.0,0",
            "M18 12c0 1.7-.7 3.2-1.8 4.2",
        )
    val mic_vocal =
        outline(
            "mic-vocal",
            "m11 7.601-5.994 8.19a1 1 0 0 0 .1 1.298l.817.818a1 1 0 0 0 1.314.087L15.09 12",
            "M16.5 21.174C15.5 20.5 14.372 20 13 20c-2.058 0-3.928 2.356-6 2-2.072-.356-2.775-3.369-1.5-4.5",
            "M11.0,7.0a5.0,5.0 0 1,0 10.0,0a5.0,5.0 0 1,0 -10.0,0",
        )
    val piano =
        outline(
            "piano",
            "M10 13v4",
            "M14 13v4",
            "M18 13v4",
            "M2 13h20",
            "M22 11.5A3.5 3.5 0 0018.5 8a3.52 3.52 0 01-3.173-2A7 7 0 002 9v10a2 2 0 002 2h16a2 2 0 002-2z",
            "M6 13v4",
        )
    val hand_metal =
        outline(
            "hand-metal",
            "M18 12.5V10a2 2 0 0 0-2-2a2 2 0 0 0-2 2v1.4",
            "M14 11V9a2 2 0 1 0-4 0v2",
            "M10 10.5V5a2 2 0 1 0-4 0v9",
            "m7 15-1.76-1.76a2 2 0 0 0-2.83 2.82l3.6 3.6C7.5 21.14 9.2 22 12 22h2a8 8 0 0 0 8-8V7a2 2 0 1 0-4 0v5",
        )
    val audio_lines =
        outline("audio-lines", "M2 10v3", "M6 6v11", "M10 3v18", "M14 8v7", "M18 5v13", "M22 10v3")
    val mic =
        outline(
            "mic",
            "M12 19v3",
            "M19 10v2a7 7 0 0 1-14 0v-2",
            "M12.0,2.0H12.0Q15.0,2.0 15.0,5.0V12.0Q15.0,15.0 12.0,15.0H12.0Q9.0,15.0 9.0,12.0V5.0Q9.0,2.0 12.0,2.0Z",
        )
    val scale =
        outline(
            "scale",
            "M12 3v18",
            "m19 8 3 8a5 5 0 0 1-6 0zV7",
            "M3 7h1a17 17 0 0 0 8-2 17 17 0 0 0 8 2h1",
            "m5 8 3 8a5 5 0 0 1-6 0zV7",
            "M7 21h10",
        )
    val sliders_vertical =
        outline(
            "sliders-vertical",
            "M10 8h4",
            "M12 21v-9",
            "M12 8V3",
            "M17 16h4",
            "M19 12V3",
            "M19 21v-5",
            "M3 14h4",
            "M5 10V3",
            "M5 21v-7",
        )

    // Mi Sound sound-wave mark, authored on the same rounded outline grid.
    val logo = outline("misound_logo", "M4 10v4M8 6v12M12 3v18M16 6v12M20 10v4")

    // Custom supplements share the source grid, stroke and rounded terminals.
    val electric_guitar =
        outline(
            "electric_guitar",
            "M13 11l6-6 2 2-6 6",
            "M19 5l1-3 2 2-1 3",
            "M13 11c-2-2-4-2-5 0l-1 2-4 2 1 5 5 1 2-4 2-1c2-1 2-3 0-5Z",
            "M7 16l1 1",
        )
    val saxophone =
        outline(
            "saxophone",
            "M7 3h3v11a3 3 0 0 0 6 0v-3h4v3a7 7 0 0 1-14 0V6L4 4",
            "M16 11l-1-3 6 1-1 2",
            "M10 8h2M10 11h2",
        )
    val harmonica =
        outline(
            "harmonica",
            "M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z",
            "M2 10h20M6 10v5M10 10v5M14 10v5M18 10v5",
        )
    val disco_ball =
        outline(
            "disco_ball",
            "M12 2v3",
            "M5 12a7 7 0 1 0 14 0a7 7 0 1 0-14 0",
            "M5 12h14M6 8h12M6 16h12",
            "M12 5c-4 4-4 10 0 14c4-4 4-10 0-14",
            "M2 4v2M1 5h2M21 19v2M20 20h2",
        )

    // Bands run bass to treble. Rounded heights depict the unchanged saved gains.
    private fun spectrum(name: String, gains: List<Int>): ImageVector =
        outline(
            name,
            *gains
                .mapIndexed { index, gain ->
                    val x = 3f + index * 3f
                    val top = 11f - gain.coerceIn(-10, 10) * 0.7f
                    "M$x,$top V20"
                }
                .toTypedArray(),
        )

    val bass_booster = spectrum("bass_booster", listOf(10, 8, -3, 0, -3, 5, 5))
    val bass_reduction = spectrum("bass_reduction", listOf(-5, -3, 0, 0, 0, 0, 0))
    val treble_reduction = spectrum("treble_reduction", listOf(0, 0, 0, 0, 0, -5, -3))
    val soft_bass = spectrum("soft_bass", listOf(3, 3, 1, -3, -3, 0, 0))
    val soft_treble = spectrum("soft_treble", listOf(0, 0, -1, -4, -4, 5, 4))
}
