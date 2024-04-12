package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Enemy(
    texture: Texture,
    private val explosionTexture: Texture,
    val initialPosition: Vector2,
    private val updatePositionDt: Enemy.(t: Float) -> Unit = { },
    nextShootingDelay: (counter: Int, time: Float) -> Float,
    initialShootingDelay: Float = nextShootingDelay(0, 0f),
    private val shot: (Enemy) -> Unit,
    private val initialHitPoints: Int = 1,
    private val invincibilityPeriod: Float = 0f,
    private val onDefeat: () -> Char?,
) : SimpleSprite(texture) {

    val internalPosition: Vector2 = initialPosition.cpy()
    var internalTimer = 0f
        private set

    var offscreenTimeToDisappear = DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR
    var hitPoints = initialHitPoints
      private set
    val isAlive
        get() = hitPoints > 0

    private var timeToMortal = 0f
    val isInvincible: Boolean
        get() = isAlive && timeToMortal > 0f

    private var shootingRepeater = DelayedRepeater(nextShootingDelay, initialShootingDelay) { _, _ -> shot(this); true }

    fun update(delta: Float) {
        if (isAlive) {
            timeToMortal -= delta
            internalTimer += delta
            updatePositionDt(internalTimer)
            setPosition(
                internalPosition.x - width / 2f,
                internalPosition.y - height / 2f
            )
            shootingRepeater.update(delta)
        } else {
            internalTimer -= delta
        }
    }

    val shouldBeRemoved: Boolean
        get() = !isAlive && internalTimer <= 0

    fun hit(): Char? {
        hitPoints -= 1
        if (isAlive) {
            timeToMortal = invincibilityPeriod
            return null
        }
        internalTimer = 0.5f
        texture = explosionTexture
        return onDefeat()
    }

    companion object {
        const val DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR = 0.5f
    }

}
