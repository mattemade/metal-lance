package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Shot(val internalPosition: Vector2, private val directionDt: (shot: Shot, time: Float) -> Unit, texture: Texture): SimpleSprite(texture) {

    val direction: Vector2 = Vector2()
    private val previousPosition = internalPosition.cpy()
    private var internalTimer = 0f

    fun update(delta: Float) {
        internalTimer += delta
        directionDt(this, internalTimer)
        previousPosition.set(internalPosition)
        internalPosition.mulAdd(direction, delta)
        setPosition((internalPosition.x - width / 2f).toInt().toFloat(), (internalPosition.y - height / 2f).toInt().toFloat())
        rotation = ((direction.angleDeg() + 45f/2f) / 45).toInt() * 45f
    }

    fun hits(position: Vector2, safeDistance: Float): Boolean {
        return internalPosition.dst(position) < safeDistance
    }

    companion object {
        const val SPEED_FAST = 300f
        const val SPEED_SLOW = 80f
    }

}
