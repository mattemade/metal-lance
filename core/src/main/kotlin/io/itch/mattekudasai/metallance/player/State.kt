package io.itch.mattekudasai.metallance.player

import io.itch.mattekudasai.metallance.player.CommandAndCounter.Companion.commandAndCounter
import kotlin.properties.ReadWriteProperty

class State {
    var flyingForward: Boolean by commandAndCounter()
    var flyingBackward: Boolean by commandAndCounter()
    var flyingUp: Boolean by commandAndCounter()
    var flyingDown: Boolean by commandAndCounter()
    var shooting: Boolean by commandAndCounter()
    var slow: Boolean by commandAndCounter()

    var x: Float = 0f
    var y: Float = 0f
    var timeFromLastShot = Float.MAX_VALUE
}
