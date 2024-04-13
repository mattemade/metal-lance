package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Enemy(
    texture: Texture,
    private val explosionTexture: Texture,
    val initialPosition: Vector2,
    private val updatePositionDt: Enemy.(t: Float) -> Unit = { },
    private val shot: (Enemy) -> Unit,
    private val initialHitPoints: Int = 1,
    private val invincibilityPeriod: Float = 0f,
    private val onDefeat: () -> Char?,
) : SimpleSprite(texture) {

    val internalPosition: Vector2 = initialPosition.cpy()
    val previousPosition: Vector2 = internalPosition.cpy()
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

    var shootingPattern: ShootingPattern? = null
        set(value) {
            value?.let {
                shootingRepeater = DelayedRepeater(it.nextDelay, it.initialDelay) { _, _ -> shot(this); true }
            }
            field = value
        }

    private var shootingRepeater: DelayedRepeater? = null


    fun update(delta: Float) {
        if (isAlive) {
            timeToMortal -= delta
            internalTimer += delta
            previousPosition.set(internalPosition)
            updatePositionDt(internalTimer)
            setPosition(
                internalPosition.x - width / 2f,
                internalPosition.y - height / 2f
            )
            shootingRepeater?.update(delta)
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
