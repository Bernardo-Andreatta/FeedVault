package com.bernardo.feedvault.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide playback state that lives only for the current process. [muted] is a single toggle
 * shared by every video player (embedded and fullscreen), so muting one mutes all; it resets to
 * muted when the app is killed and restarted (session-only — intentionally not persisted).
 */
object PlaybackSession {
    var muted by mutableStateOf(true)
}
