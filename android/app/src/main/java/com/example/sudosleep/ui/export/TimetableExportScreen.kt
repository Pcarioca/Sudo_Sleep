package com.example.sudosleep.ui.export

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sudosleep.data.local.pojo.TimetableEntryWithSubject
import com.example.sudosleep.util.TimetableExporter
import com.example.sudosleep.util.WallpaperStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableExportScreen(
    entries: List<TimetableEntryWithSubject>,
    studentName: String,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedStyle by remember { mutableStateOf(WallpaperStyle.COZY_PASTEL) }
    var includeWeekends by remember { mutableStateOf(false) }
    var lockScreenClockPadding by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }

    val previewBitmap = remember(entries, selectedStyle, studentName, includeWeekends, lockScreenClockPadding) {
        TimetableExporter.renderWallpaperBitmap(
            entries = entries,
            style = selectedStyle,
            studentName = studentName,
            includeWeekends = includeWeekends,
            lockScreenClockPadding = lockScreenClockPadding
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Phone Wallpaper Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📱 Live Wallpaper & Timetable Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Formatted in crisp 1080×2400 HD with lock-screen clock clearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Wallpaper Timetable Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(340.dp)
                        .aspectRatio(9f / 20f)
                        .clip(RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wallpaper Style Selector
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperStyle.entries.forEach { style ->
                        FilterChip(
                            selected = selectedStyle == style,
                            onClick = { selectedStyle = style },
                            label = { Text(style.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles for Lock Screen Padding & Weekends
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock Screen Clock Spacing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Leaves room at the top for your phone's lock-screen clock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = lockScreenClockPadding,
                        onCheckedChange = { lockScreenClockPadding = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Saturday & Sunday Columns",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Include weekend columns even if empty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeWeekends,
                        onCheckedChange = { includeWeekends = it }
                    )
                }
            }
        }

        // PNG Wallpaper Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🖼️ Phone Wallpaper (PNG)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    enabled = !isExporting,
                    onClick = {
                        isExporting = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                TimetableExporter.applyAsPhoneWallpaper(
                                    context = context,
                                    bitmap = previewBitmap,
                                    whichFlags = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                )
                            }
                            isExporting = false
                            snackbarHostState.showSnackbar(
                                if (ok) "✨ Timetable applied as your Phone Wallpaper!"
                                else "Saved! You can also set it from your Gallery."
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Phone Wallpaper Now", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    TimetableExporter.saveWallpaperPngToMediaStore(context, previewBitmap)
                                }
                                isExporting = false
                                if (uri != null) {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "PNG saved to Pictures/SudoSleep 📸",
                                        actionLabel = "Share"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        TimetableExporter.shareExportedFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "image/png",
                                            title = "Share Timetable Wallpaper"
                                        )
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PNG")
                    }

                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    TimetableExporter.saveWallpaperPngToMediaStore(context, previewBitmap)
                                }
                                isExporting = false
                                if (uri != null) {
                                    TimetableExporter.shareExportedFile(
                                        context = context,
                                        uri = uri,
                                        mimeType = "image/png",
                                        title = "Share Timetable Wallpaper"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PNG")
                    }
                }
            }
        }

        // PDF Print-Ready Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📄 Print-Ready Weekly Timetable (PDF)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generates a clean A4 Landscape PDF document ready for printing or sharing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    enabled = !isExporting,
                    onClick = {
                        isExporting = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                TimetableExporter.exportWeeklyPdfToMediaStore(
                                    context = context,
                                    entries = entries,
                                    studentName = studentName
                                )
                            }
                            isExporting = false
                            if (uri != null) {
                                val res = snackbarHostState.showSnackbar(
                                    message = "PDF saved to Downloads/SudoSleep 📄",
                                    actionLabel = "Share / Print"
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    TimetableExporter.shareExportedFile(
                                        context = context,
                                        uri = uri,
                                        mimeType = "application/pdf",
                                        title = "Share Weekly Timetable PDF"
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Save Weekly PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
