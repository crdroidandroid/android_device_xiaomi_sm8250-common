/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.speaker

import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class ClearSpeakerActivity : PartsActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var manager: AudioManager? = null
    private var playing by mutableStateOf(false)

    @Composable
    override fun Content() {
        PartsPage(stringResource(R.string.clear_speaker_title), { finish() }) {
            ToggleCard(
                stringResource(R.string.clear_speaker_title),
                stringResource(R.string.clear_speaker_summary),
                playing,
                main = true,
            ) {
                if (!it) stopPlaying()
                else if (!startPlaying())
                    Toast.makeText(
                            this@ClearSpeakerActivity,
                            R.string.parts_apply_failed,
                            Toast.LENGTH_SHORT,
                        )
                        .show()
            }
            Text(
                stringResource(R.string.clear_speaker_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    override fun onStop() {
        stopPlaying()
        super.onStop()
    }

    private fun startPlaying(): Boolean {
        stopPlaying()
        val audio = getSystemService(AudioManager::class.java) ?: return false
        manager = audio
        if (audio.mode != AudioManager.MODE_NORMAL) return false
        val speaker =
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } ?: return false
        volumeControlStream = AudioManager.STREAM_MUSIC
        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        try {
            val request =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(
                        { change -> if (change != AudioManager.AUDIOFOCUS_GAIN) stopPlaying() },
                        handler,
                    )
                    .build()
            focus = request
            if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                stopPlaying()
                return false
            }
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(attributes)
            resources.openRawResourceFd(R.raw.clear_speaker_sound).use { media.setDataSource(it) }
            if (!media.setPreferredDevice(speaker)) {
                stopPlaying()
                return false
            }
            media.addOnRoutingChangedListener(
                { router ->
                    val routed = router.routedDevice
                    if (routed != null && routed.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                        stopPlaying()
                },
                handler,
            )
            media.setOnErrorListener { _, _, _ ->
                stopPlaying()
                true
            }
            media.isLooping = true
            media.setVolume(1f, 1f)
            media.prepare()
            media.start()
            playing = true
            handler.postDelayed({ stopPlaying() }, 30000)
            return true
        } catch (error: Exception) {
            Log.e("ClearSpeaker", "Failed to play speaker cleaning sound", error)
            stopPlaying()
            return false
        }
    }

    private fun stopPlaying() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        focus?.let { manager?.abandonAudioFocusRequest(it) }
        focus = null
        playing = false
    }
}
