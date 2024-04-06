package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.player.Controls.isBackward
import io.itch.mattekudasai.metallance.player.Controls.isDown
import io.itch.mattekudasai.metallance.player.Controls.isForward
import io.itch.mattekudasai.metallance.player.Controls.isShoot
import io.itch.mattekudasai.metallance.player.Controls.isSlow
import io.itch.mattekudasai.metallance.player.Controls.isUp
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import ktx.app.KtxInputAdapter

class Flagship : SimpleSprite("ship.png"), KtxInputAdapter {

    private val state = State()

    override fun keyDown(keycode: Int): Boolean {
        with(state) {
            when {
                keycode.isForward -> flyingForward = true
                keycode.isBackward -> flyingBackward = true
                keycode.isUp -> flyingUp = true
                keycode.isDown -> flyingDown = true
                keycode.isShoot -> shooting = true
                keycode.isSlow -> slow = true
            }
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        with(state) {
            when {
                keycode.isForward -> flyingForward = false
                keycode.isBackward -> flyingBackward = false
                keycode.isUp -> flyingUp = false
                keycode.isDown -> flyingDown = false
                keycode.isShoot -> shooting = false
                keycode.isSlow -> slow = false
            }
        }
        return true
    }

    private val horizontalSpeedMap = mapOf(
        false to mapOf(
            // not moving forward
            false to 0f, // not moving backward
            true to -HORIZONTAL_SPEED, // moving backward
        ),
        true to mapOf(
            // moving forward
            false to HORIZONTAL_SPEED, // not moving backward
            true to 0f, // moving backward
        )
    )
    private val verticalSpeedMap = mapOf(
        false to mapOf(
            // not moving up
            false to 0f, // not moving down
            true to -VERTICAL_SPEED, // moving backward
        ),
        true to mapOf(
            // moving forward
            false to VERTICAL_SPEED, // not moving backward
            true to 0f, // moving backward
        )
    )
    private val movingTo = Vector2()

    fun update(delta: Float) {
        movingTo.set(
            delta * horizontalSpeedMap[state.flyingForward]!![state.flyingBackward]!!,
            delta * verticalSpeedMap[state.flyingUp]!![state.flyingDown]!!
        )
        if (movingTo.x != 0f && movingTo.y != 0f) {
            movingTo.scl(DIAGONAL_SPEED_FACTOR)
        }
        if (state.slow) {
            movingTo.scl(SLOWING_FACTOR)
        }
        state.x += movingTo.x
        state.y += movingTo.y
        setPosition(state.x.toInt().toFloat(), state.y.toInt().toFloat())
    }

    companion object {
        const val SPEED_LIMIT = 200f
        const val HORIZONTAL_SPEED = SPEED_LIMIT
        const val VERTICAL_SPEED = SPEED_LIMIT * 0.66f
        const val DIAGONAL_SPEED_FACTOR = SPEED_LIMIT / (HORIZONTAL_SPEED + VERTICAL_SPEED)
        const val SLOWING_FACTOR = 0.5f
    }
}
