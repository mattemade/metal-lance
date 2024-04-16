package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import io.itch.mattekudasai.metallance.util.sound.playSingleLow
import kotlin.math.max

class Enemy(
    texture: Texture,
    private val explosionTexture: Texture,
    val initialPosition: Vector2,
    private val updatePositionDt: Enemy.(t: Float) -> Any = { },
    private val shot: (Enemy) -> Unit,
    private val initialHitPoints: Int = 1,
    private val invincibilityPeriod: Float = 0f,
    val onRemoved: () -> Unit,
    private val onDefeat: () -> Char?,
    private val hitSound: Sound,
    private val explodeSound: Sound,
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

    private var lastHitSound = -1L

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
            setPosition(internalPosition.x, internalPosition.y)
            shootingRepeater?.update(delta)
        } else {
            internalTimer -= delta
        }
    }

    val shouldBeRemoved: Boolean
        get() = !isAlive && internalTimer <= 0

    fun hit(damage: Int = 1): Char? {
        if (lastHitSound > -1) {
            hitSound.stop(lastHitSound)
            lastHitSound = -1
        }
        hitPoints = max(0, hitPoints - damage)
        if (isAlive) {
            timeToMortal = invincibilityPeriod
            lastHitSound = hitSound.playSingleLow(lastHitSound, volume = 0.2f)
            return null
        }
        explodeSound.playSingleLow(volume = 0.15f)
        internalTimer = 0.5f
        texture = explosionTexture
        return onDefeat()
    }

    companion object {
        const val DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR = 0.5f
    }

}
