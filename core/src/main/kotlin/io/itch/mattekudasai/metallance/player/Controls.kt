package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.Input.Keys

object Controls {

    private val forwardKeycodes = setOf(Keys.D, Keys.RIGHT)
    private val backwardKeycodes = setOf(Keys.A, Keys.LEFT)
    private val upKeycodes = setOf(Keys.W, Keys.UP)
    private val downKeycodes = setOf(Keys.S, Keys.DOWN)
    private val shootKeycodes = setOf(Keys.K, Keys.Z)
    private val lanceKeycodes = setOf(Keys.J, Keys.X)
    private val slowKeycodes = setOf(Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT)

    private val allKeys =
        forwardKeycodes + backwardKeycodes + upKeycodes + downKeycodes + shootKeycodes + slowKeycodes + lanceKeycodes

    val Int.isForward: Boolean get() = forwardKeycodes.contains(this)
    val Int.isBackward: Boolean get() = backwardKeycodes.contains(this)
    val Int.isUp: Boolean get() = upKeycodes.contains(this)
    val Int.isDown: Boolean get() = downKeycodes.contains(this)
    val Int.isShoot: Boolean get() = shootKeycodes.contains(this)
    val Int.isLance: Boolean get() = lanceKeycodes.contains(this)
    val Int.isSlow: Boolean get() = slowKeycodes.contains(this)

    val Int.isAnyKey: Boolean get() = allKeys.contains(this)
}
