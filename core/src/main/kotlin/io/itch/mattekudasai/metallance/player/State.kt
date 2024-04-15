package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.player.CommandAndCounter.Companion.commandAndCounter

class State(initialX: Float, initialY: Float) {
    var flyingForward: Boolean by commandAndCounter()
    var flyingBackward: Boolean by commandAndCounter()
    var flyingUp: Boolean by commandAndCounter()
    var flyingDown: Boolean by commandAndCounter()
    var wantToLance: Boolean by commandAndCounter()
    var shooting: Boolean by commandAndCounter()
    var slow: Boolean by commandAndCounter()

    val startLancingPosition = Vector2(initialX, initialY)
    val endLancingPosition = Vector2(initialX, initialY)
    val position = Vector2(initialX, initialY)
    var timeFromLastShot = Float.MAX_VALUE
}
