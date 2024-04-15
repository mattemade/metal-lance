package io.itch.mattekudasai.metallance.util.sound

import com.badlogic.gdx.audio.Sound

fun Sound.playSingleLow(oldId: Long = -2L, volume: Float = 0.1f): Long {
    if (oldId == -2L) {
        stop()
    } else if (oldId >= 0) {
        stop(oldId)
    }
    return playLow(volume)
}

fun Sound.playLow(volume: Float = 0.1f): Long {
    val soundId = play()
    setVolume(soundId, volume)
    return soundId
}
