package com.neoncartel.drugwars.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.neoncartel.drugwars.R

class GameAudio(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds = mapOf(
        Cue.Click to soundPool.load(context, R.raw.ui_click, 1),
        Cue.Buy to soundPool.load(context, R.raw.buy, 1),
        Cue.Sell to soundPool.load(context, R.raw.sell, 1),
        Cue.Siren to soundPool.load(context, R.raw.police_siren, 1),
        Cue.Travel to soundPool.load(context, R.raw.travel, 1),
        Cue.Danger to soundPool.load(context, R.raw.gang_encounter, 1),
        Cue.Ambient to soundPool.load(context, R.raw.ambient_pulse, 1),
    )

    fun play(cue: Cue, enabled: Boolean) {
        if (!enabled) return
        sounds[cue]?.let { soundId ->
            soundPool.play(soundId, 0.55f, 0.55f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}

enum class Cue {
    Click,
    Buy,
    Sell,
    Siren,
    Travel,
    Danger,
    Ambient,
}
