package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Enemy(
    texture: Texture,
    private val explosionTexture: Texture,
    val initialPosition: Vector2,
    private val updatePositionDt: Enemy.(t: Float) -> Unit = { },
    nextShootingDelay: () -> Float,
    private val shot: (Enemy) -> Unit,
) : SimpleSprite(texture) {

    val internalPosition: Vector2 = initialPosition.cpy()
    var internalTimer = 0f
        private set
    var isAlive = true
        private set
    private var shootingRepeater = DelayedRepeater(nextShootingDelay) { shot(this) }

    fun update(delta: Float) {
        if (isAlive) {
            internalTimer += delta
            updatePositionDt(internalTimer)
            setPosition(
                (internalPosition.x - width / 2f).toInt().toFloat(),
                (internalPosition.y - height / 2f).toInt().toFloat()
            )
            shootingRepeater.update(delta)
        } else {
            internalTimer -= delta
        }
    }

    val shouldBeRemoved: Boolean
        get() = !isAlive && internalTimer <= 0
    fun explode() {
        isAlive = false
        internalTimer = 0.5f
        texture = explosionTexture
    }

}
