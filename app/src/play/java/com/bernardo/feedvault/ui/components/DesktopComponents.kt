package com.bernardo.feedvault.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bernardo.feedvault.data.DesktopFile
import com.bernardo.feedvault.ui.DesktopViewModel

/**
 * Play-flavor stub. The Desktop companion feature is compiled out of the store build
 * (ENABLE_DESKTOP = false), but MainActivity still references this symbol behind the
 * flag, so an empty implementation keeps the play variant compiling without pulling
 * in the QR-scanner (zxing) dependency. R8 removes it from the shipped bundle.
 */
@Composable
fun DesktopScreen(
    viewModel: DesktopViewModel,
    onSaveFile: (DesktopFile) -> Unit,
    onSaveAll: (List<DesktopFile>) -> Unit = {},
    modifier: Modifier = Modifier
) {
}
