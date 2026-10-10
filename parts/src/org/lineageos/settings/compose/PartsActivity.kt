/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.lineageos.settings.R

abstract class PartsActivity : ComponentActivity() {
    @Composable abstract fun Content()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isSystemInDarkTheme()
            val base = if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            // Use the same platform surface tokens as SettingsLib, including OEM overlays.
            fun token(name: String, fallback: Color): Color {
                val id =
                    resources.getIdentifier(
                        "system_${name}_${if (dark) "dark" else "light"}",
                        "color",
                        "android",
                    )
                return if (id != 0) Color(getColor(id)) else fallback
            }
            val colors =
                base.copy(
                    background = token("surface_container", base.surfaceContainer),
                    surface = token("surface_container", base.surfaceContainer),
                    surfaceBright = token("surface_bright", base.surfaceBright),
                    surfaceContainerHigh =
                        token("surface_container_high", base.surfaceContainerHigh),
                    onSurface = token("on_surface", base.onSurface),
                    onSurfaceVariant = token("on_surface_variant", base.onSurfaceVariant),
                )
            fun family(name: String) =
                FontFamily(
                    android.graphics.Typeface.create(
                        "variable-$name",
                        android.graphics.Typeface.NORMAL,
                    )
                )
            val typography =
                Typography().let { defaults ->
                    defaults.copy(
                        titleLarge = defaults.titleLarge.copy(fontFamily = family("title-large")),
                        titleMedium =
                            defaults.titleMedium.copy(fontFamily = family("title-medium")),
                        bodyLarge = defaults.bodyLarge.copy(fontFamily = family("body-large")),
                        bodyMedium = defaults.bodyMedium.copy(fontFamily = family("body-medium")),
                        headlineSmall =
                            defaults.headlineSmall.copy(fontFamily = family("headline-small")),
                    )
                }
            MaterialExpressiveTheme(
                colorScheme = colors,
                typography = typography,
                motionScheme = MotionScheme.expressive(),
            ) {
                Content()
            }
        }
    }
}

/** Small UI icons stay in Compose instead of adding a new build dependency. */
object PartsIcons {
    val Check =
        ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(9f, 16.2f)
                    lineTo(4.8f, 12f)
                    lineTo(3.4f, 13.4f)
                    lineTo(9f, 19f)
                    lineTo(21f, 7f)
                    lineTo(19.6f, 5.6f)
                    close()
                }
            }
            .build()
    val Close =
        ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(18.3f, 5.7f)
                    lineTo(12f, 12f)
                    lineTo(5.7f, 5.7f)
                    lineTo(4.3f, 7.1f)
                    lineTo(10.6f, 13.4f)
                    lineTo(4.3f, 19.7f)
                    lineTo(5.7f, 21.1f)
                    lineTo(12f, 14.8f)
                    lineTo(18.3f, 21.1f)
                    lineTo(19.7f, 19.7f)
                    lineTo(13.4f, 13.4f)
                    lineTo(19.7f, 7.1f)
                    close()
                }
            }
            .build()
    val Back =
        ImageVector.Builder("Back", 24.dp, 24.dp, 24f, 24f, autoMirror = true)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(20f, 11f)
                    horizontalLineTo(7.83f)
                    lineTo(13.42f, 5.41f)
                    lineTo(12f, 4f)
                    lineTo(4f, 12f)
                    lineTo(12f, 20f)
                    lineTo(13.42f, 18.59f)
                    lineTo(7.83f, 13f)
                    horizontalLineTo(20f)
                    close()
                }
            }
            .build()
    val Chevron =
        ImageVector.Builder("Chevron", 24.dp, 24.dp, 24f, 24f, autoMirror = true)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(9.3f, 6.7f)
                    lineTo(14.6f, 12f)
                    lineTo(9.3f, 17.3f)
                    lineTo(10.7f, 18.7f)
                    lineTo(17.4f, 12f)
                    lineTo(10.7f, 5.3f)
                    close()
                }
            }
            .build()
    val Info =
        ImageVector.Builder("Info", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                    moveTo(12f, 2f)
                    curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                    curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                    curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                    curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                    close()
                }
                path(fill = SolidColor(Color.Black)) {
                    moveTo(11f, 10f)
                    horizontalLineTo(13f)
                    verticalLineTo(18f)
                    horizontalLineTo(11f)
                    close()
                    moveTo(11f, 6f)
                    horizontalLineTo(13f)
                    verticalLineTo(8f)
                    horizontalLineTo(11f)
                    close()
                }
            }
            .build()
    val Tune =
        ImageVector.Builder("Tune", 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3f, 5f)
                    horizontalLineTo(21f)
                    verticalLineTo(7f)
                    horizontalLineTo(3f)
                    close()
                    moveTo(3f, 11f)
                    horizontalLineTo(21f)
                    verticalLineTo(13f)
                    horizontalLineTo(3f)
                    close()
                    moveTo(3f, 17f)
                    horizontalLineTo(21f)
                    verticalLineTo(19f)
                    horizontalLineTo(3f)
                    close()
                    moveTo(6f, 3f)
                    horizontalLineTo(9f)
                    verticalLineTo(9f)
                    horizontalLineTo(6f)
                    close()
                    moveTo(15f, 9f)
                    horizontalLineTo(18f)
                    verticalLineTo(15f)
                    horizontalLineTo(15f)
                    close()
                    moveTo(8f, 15f)
                    horizontalLineTo(11f)
                    verticalLineTo(21f)
                    horizontalLineTo(8f)
                    close()
                }
            }
            .build()
}

/** Refresh hardware-owned state on every resume, including returning from another page. */
@Composable
fun OnResume(refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latest()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) latest()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsScaffold(
    title: String,
    back: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = back,
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surfaceContainerHigh,
                                contentColor = colors.onSurfaceVariant,
                            ),
                    ) {
                        Icon(PartsIcons.Back, stringResource(R.string.parts_back))
                    }
                },
                actions = actions,
                expandedHeight = maxOf(64.dp, (56 * LocalDensity.current.fontScale + 8).dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
        content = content,
    )
}

@Composable
fun PartsPage(
    title: String,
    back: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    PartsScaffold(title, back, actions) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 840.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 12.dp),
    )
}

@Composable
fun InfoCard(title: String, summary: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ToggleCard(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    main: Boolean = false,
    change: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = rememberPartsHaptics()
    Card(
        Modifier.fillMaxWidth().padding(vertical = if (main) 8.dp else 0.dp),
        shape = if (main) RoundedCornerShape(42.dp) else LocalPreferenceShape.current,
        colors =
            CardDefaults.cardColors(
                containerColor = if (main) colors.primaryContainer else colors.surfaceBright
            ),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = {
                        haptics.selection()
                        change(it)
                    },
                )
                .heightIn(min = 72.dp)
                .padding(
                    start = if (main) 32.dp else 16.dp,
                    end = if (main) 20.dp else 16.dp,
                    top = 16.dp,
                    bottom = 16.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        (if (main) colors.onPrimaryContainer else colors.onSurface).copy(
                            alpha = if (enabled) 1f else 0.5f
                        ),
                )
                if (summary.isNotEmpty())
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            (if (main) colors.onPrimaryContainer else colors.onSurfaceVariant).copy(
                                alpha = if (enabled) 1f else 0.5f
                            ),
                    )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        if (checked) PartsIcons.Check else PartsIcons.Close,
                        null,
                        Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
fun ChoiceCard(
    title: String,
    value: String,
    choices: List<Pair<String, String>>,
    enabled: Boolean = true,
    descriptions: Map<String, String> = emptyMap(),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDescription: Boolean = true,
    choiceIcon: (@Composable (String, Boolean) -> Unit)? = null,
    change: (String) -> Unit,
) {
    var opened by rememberSaveable(title) { mutableStateOf(false) }
    val haptics = rememberPartsHaptics()
    val selected = choices.firstOrNull { it.first == value }?.second ?: value
    Card(
        onClick = { opened = true },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = LocalPreferenceShape.current,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading()
            else if (choiceIcon != null) {
                Box(Modifier.alpha(if (enabled) 1f else 0.5f)) { choiceIcon(value, true) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f),
                )
                Text(
                    selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.5f
                        ),
                )
                if (showDescription)
                    descriptions[value]?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (enabled) 1f else 0.5f
                                ),
                        )
                    }
            }
            if (trailing != null) trailing()
            else
                Icon(
                    PartsIcons.Chevron,
                    null,
                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.5f
                        ),
                )
        }
    }
    if (opened)
        AlertDialog(
            onDismissRequest = { opened = false },
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    choices.forEach { (id, name) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = value == id,
                                    role = Role.RadioButton,
                                    onClick = {
                                        haptics.selection()
                                        change(id)
                                        opened = false
                                    },
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (choiceIcon == null) {
                                RadioButton(selected = value == id, onClick = null)
                            } else {
                                choiceIcon(id, value == id)
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                descriptions[id]?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
        )
}

@Composable
fun ResourceChoices(entries: Int, values: Int): List<Pair<String, String>> {
    val res = LocalContext.current.resources
    return res.getStringArray(values).zip(res.getStringArray(entries)).map { it.first to it.second }
}

@Composable
fun rememberPartsHaptics(): PartsHaptics {
    val view = LocalView.current
    return remember(view) { PartsHaptics(view) }
}

/** Adjacent preferences share the Settings first/middle/last row geometry. */
val LocalPreferenceShape = staticCompositionLocalOf { RoundedCornerShape(24.dp) }

@Composable
fun PreferenceRow(index: Int, count: Int, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPreferenceShape provides
            RoundedCornerShape(
                topStart = if (index == 0) 24.dp else 4.dp,
                topEnd = if (index == 0) 24.dp else 4.dp,
                bottomStart = if (index == count - 1) 24.dp else 4.dp,
                bottomEnd = if (index == count - 1) 24.dp else 4.dp,
            ),
        content = content,
    )
}
