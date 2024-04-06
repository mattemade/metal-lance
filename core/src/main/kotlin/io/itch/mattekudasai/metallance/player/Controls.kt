package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.Input.Keys

object Controls {

    private val forwardKeycodes = setOf(Keys.D, Keys.RIGHT)
    private val backwardKeycodes = setOf(Keys.A, Keys.LEFT)
    private val upKeycodes = setOf(Keys.W, Keys.UP)
    private val downKeycodes = setOf(Keys.S, Keys.DOWN)
    private val shootKeycodes = setOf(Keys.J, Keys.X, Keys.SPACE)
    private val slowKeycodes = setOf(Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT, Keys.K, Keys.Z)

    val Int.isForward: Boolean get() = forwardKeycodes.contains(this)
    val Int.isBackward: Boolean get() = backwardKeycodes.contains(this)
    val Int.isUp: Boolean get() = upKeycodes.contains(this)
    val Int.isDown: Boolean get() = downKeycodes.contains(this)
    val Int.isShoot: Boolean get() = shootKeycodes.contains(this)
    val Int.isSlow: Boolean get() = slowKeycodes.contains(this)
}
