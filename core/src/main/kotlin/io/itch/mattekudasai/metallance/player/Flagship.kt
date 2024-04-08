package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.player.Controls.isBackward
import io.itch.mattekudasai.metallance.player.Controls.isDown
import io.itch.mattekudasai.metallance.player.Controls.isForward
import io.itch.mattekudasai.metallance.player.Controls.isShoot
import io.itch.mattekudasai.metallance.player.Controls.isSlow
import io.itch.mattekudasai.metallance.player.Controls.isUp
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import ktx.app.KtxInputAdapter

// TODO: think about dynamic world width/height
class Flagship(
    private val worldWidth: Float,
    private val worldHeight: Float,
    private val explosionTexture: Texture,
    private val shot: () -> Unit
) :
    SimpleSprite("ship.png"), KtxInputAdapter {

    private val state = State(40f, worldHeight / 2f)
    val internalPosition: Vector2 get() = state.position
    val rearPosition = Vector2()
    val frontPosition = Vector2()
    private val normalTexture = texture

    private val halfWidth = width / 2f
    private val halfHeight = height / 2f
    private var shootingRate = SHOOTING_RATE
    var isAlive = true
      private set

    val isInvincible: Boolean
        get() = isAlive && timeToStartOver > 0f

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


    private val movingTo = Vector2()
    private var timeToStartOver = 0f

    fun update(delta: Float) {
        timeToStartOver -= delta
        if (!isAlive) {
            if (timeToStartOver > 0f) {
                return
            }
            startOver()
            // not returning!
        }
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
        state.position.add(movingTo)
        with(state.position) {
            add(movingTo)
            if (x < halfWidth) {
                x = halfWidth
            } else if (x > worldWidth - halfWidth) {
                x = worldWidth - halfWidth
            }
            if (y < halfHeight) {
                y = halfHeight
            } else if (y > worldHeight - halfHeight) {
                y = worldHeight - halfHeight
            }
        }
        setPosition(
            (state.position.x - halfWidth).toInt().toFloat(),
            (state.position.y - halfHeight).toInt().toFloat()
        )

        if (state.timeFromLastShot > shootingRate && state.shooting) {
            state.timeFromLastShot = 0f
            shot()
        }
        state.timeFromLastShot += delta
        rearPosition.set(state.position).sub(3f, 0f)
        frontPosition.set(state.position).add(3f, 0f)
    }

    fun powerUp() {
        shootingRate /= 2f
    }

    // TODO: maybe reuse is for game over?
    fun explode() {
        isAlive = false
        texture = explosionTexture
        setBounds(0f, 0f, explosionTexture.width.toFloat(), explosionTexture.height.toFloat())
        setPosition(state.position.x - explosionTexture.width / 2, state.position.y - explosionTexture.height / 2)
        timeToStartOver = 1f
    }

    fun startOver() {
        isAlive = true
        texture = normalTexture
        setBounds(0f, 0f, normalTexture.width.toFloat(), normalTexture.height.toFloat())
        timeToStartOver = 2f
        //state.position.set(40f, worldHeight / 2f)
        shootingRate = SHOOTING_RATE
    }

    companion object {
        const val SPEED_LIMIT = 100f
        const val HORIZONTAL_SPEED = SPEED_LIMIT
        const val VERTICAL_SPEED = SPEED_LIMIT * 0.66f
        const val DIAGONAL_SPEED_FACTOR = SPEED_LIMIT / (HORIZONTAL_SPEED + VERTICAL_SPEED)
        const val SLOWING_FACTOR = 0.5f
        const val SHOOTING_RATE = 0.5f

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
    }
}
