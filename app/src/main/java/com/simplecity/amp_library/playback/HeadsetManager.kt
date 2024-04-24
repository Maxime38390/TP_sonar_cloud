package com.simplecity.amp_library.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class HeadsetManager(
    private val playbackManager: PlaybackManager,
    private val playbackSettingsManager: PlaybackSettingsManager
) {

    private var headsetReceiver: BroadcastReceiver? = null

    fun registerHeadsetPlugReceiver(context: Context) {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!isInitialStickyBroadcast && intent.hasExtra("state")) {
                    val state = intent.getIntExtra("state", 0)
                    if (state == 0 && playbackSettingsManager.pauseOnHeadsetDisconnect) {
                        playbackManager.pause(false)
                    } else if (state == 1 && playbackSettingsManager.playOnHeadsetConnect) {
                        playbackManager.play()
                    }
                }
            }
        }
        context.registerReceiver(headsetReceiver, filter)
    }


    fun unregisterHeadsetPlugReceiver(context: Context) {
        context.unregisterReceiver(headsetReceiver)
    }
}
