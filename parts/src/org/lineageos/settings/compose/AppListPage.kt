/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.settings.R

data class PartsApp(val packageName: String, val label: String)

@Composable
fun rememberLauncherApps(): List<PartsApp> {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    OnResume { revision++ }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    revision++
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val apps by
        produceState(emptyList<PartsApp>(), context, revision) {
            value =
                withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val collator = Collator.getInstance()
                    pm.queryIntentActivities(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                            0,
                        )
                        .map {
                            PartsApp(
                                it.activityInfo.packageName,
                                it.activityInfo.applicationInfo.loadLabel(pm).toString(),
                            )
                        }
                        .distinctBy { it.packageName }
                        .sortedWith { a, b -> collator.compare(a.label, b.label) }
                }
        }
    return apps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListPage(
    title: String,
    back: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    header: @Composable () -> Unit = {},
    row: @Composable (PartsApp) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    val apps =
        rememberLauncherApps().filter {
            it.label.contains(search, true) || it.packageName.contains(search, true)
        }
    PartsScaffold(title, back, actions) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxSize().imePadding()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.parts_search_apps)) },
                    shape = RoundedCornerShape(42.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item { header() }
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        PreferenceRow(index, apps.size) { row(app) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(app: PartsApp) {
    val context = LocalContext.current
    val size = with(LocalDensity.current) { 40.dp.roundToPx() }
    val icon by
        produceState<ImageBitmap?>(null, app.packageName, size) {
            value =
                withContext(Dispatchers.IO) {
                    try {
                        context.packageManager
                            .getApplicationIcon(app.packageName)
                            .toBitmap(size, size)
                            .asImageBitmap()
                    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                        null
                    }
                }
        }
    val bitmap = icon
    if (bitmap != null) Image(bitmap, null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
    else
        Box(Modifier.size(40.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(app.label.take(1), style = MaterialTheme.typography.titleLarge)
        }
}

@Composable
fun AppChoiceCard(
    app: PartsApp,
    value: String,
    choices: List<Pair<String, String>>,
    descriptions: Map<String, String> = emptyMap(),
    trailing: (@Composable () -> Unit)? = null,
    choiceIcon: (@Composable (String, Boolean) -> Unit)? = null,
    change: (String) -> Unit,
) {
    ChoiceCard(
        app.label,
        value,
        choices,
        descriptions = descriptions,
        leading = { AppIcon(app) },
        trailing = trailing,
        showDescription = false,
        choiceIcon = choiceIcon,
        change = change,
    )
}
