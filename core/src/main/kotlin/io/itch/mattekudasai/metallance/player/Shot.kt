package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Shot(
    val internalPosition: Vector2,
    private val directionDt: ((shot: Shot, time: Float, delta: Float) -> Unit)? = null,
    texture: Texture,
    private val isRotating: Boolean = true,
    initialDirection: Vector2? = null
) : SimpleSprite(texture) {

    val direction: Vector2 = initialDirection ?: Vector2()
    private val previousPosition = internalPosition.cpy()
    var internalTimer = 0f
        private set

    var deadTime = 3f // time to keep the shot off-screen, in case it will return back

    fun update(delta: Float) {
        internalTimer += delta
        directionDt?.let {
            it.invoke(this, internalTimer, delta)
            previousPosition.set(internalPosition)
        }
        internalPosition.mulAdd(direction, delta)
        setPosition(
            (internalPosition.x - width / 2f).toInt().toFloat(),
            (internalPosition.y - height / 2f).toInt().toFloat()
        )
        if (isRotating) {
            rotation = ((direction.angleDeg() + 45f / 2f) / 45).toInt() * 45f
        }
    }

    fun hits(position: Vector2, safeDistance: Float): Boolean {
        return internalPosition.dst(position) < safeDistance
    }

    companion object {
        const val SPEED_FAST = 300f
        const val SPEED_SLOW = 80f
        const val SPEED_POWER_UP = 40f
    }

}
