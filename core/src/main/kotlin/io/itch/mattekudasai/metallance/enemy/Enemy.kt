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
    private val updatePositionDt: List<Enemy.(t: Float) -> Any> = emptyList(),
    private val shot: (Enemy) -> Unit,
    val initialHitPoints: Int = 1,
    private val invincibilityPeriod: Float = 0f,
    val onRemoved: (Enemy) -> Unit,
    private val onStageDefeat: (Int) -> Char?,
    private val onDefeat: (Enemy) -> Char?,
    private val hitSound: Sound,
    private val explodeSound: Sound,
    val isBoss: Boolean,
    val isBaloon: Boolean,
    val isTopShell: Boolean = false,
    val keepOffscreen: Boolean = false,
    val isBottomShell: Boolean = false,
    private val screaming: (remainingFactor: Float) -> Unit,
    private val anchor: Triple<Enemy, Vector2, () -> Float>? = null,
) : SimpleSprite(texture) {

    val isShell = isTopShell || isBottomShell

    val internalPosition: Vector2 = initialPosition.cpy()
    val previousPosition: Vector2 = internalPosition.cpy()
    var internalTimer = 0f
        private set

    var offscreenTimeToDisappear = if (isShell || keepOffscreen) Float.MAX_VALUE else DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR
    var hitPoints = initialHitPoints
        //private set // making it public just for the 2nd phase of the last boss
    val isAlive
        get() = hitPoints > 0

    var timeToMortal = 0f // public set as a hack to allow the last piece of the final boss not to be killed by a random bullet before its shield is ready
    val isInvincible: Boolean
        get() = isAlive && (timeToMortal > 0f || scream > 0f || stageResetsIn > 0f)

    private var lastHitSound = -1L
    private var currentStage = 0
    private var previousStageTimer = 0f
    private var currentStageTimer = 0f

    var shootingPattern: ShootingPattern? = null
        set(value) {
            value?.let {
                shootingRepeater = DelayedRepeater(it.nextDelay, it.initialDelay) { _, _, _-> shot(this); true }
            }
            field = value
        }

    private var shootingRepeater: DelayedRepeater? = null

    private val stageSize = if (updatePositionDt.isEmpty()) initialHitPoints else initialHitPoints / updatePositionDt.size // e.g. 5 for 2 stage with 10 hp
    private var scream = 0f
    private var stageResetsIn = 0f
    private var transitionVector = Vector2()

    fun updateShootingOnly(delta: Float) {
        previousPosition.set(internalPosition)
        shootingRepeater?.update(delta)
    }

    fun update(delta: Float) {
        if (anchor != null) {
            if (!isAlive) {
                internalTimer -= delta
            }
            timeToMortal -= delta
            previousPosition.set(internalPosition)
            internalPosition.set(anchor.first.internalPosition).add(anchor.second).add(0f, anchor.third())
            setPosition(internalPosition.x, internalPosition.y)
            shootingRepeater?.update(delta)
            return
        }
        if (isAlive) {
            timeToMortal -= delta
            internalTimer += delta
            previousPosition.set(internalPosition)
            val newStage = (initialHitPoints - hitPoints) / stageSize
            if (currentStage != newStage) {
                currentStage = newStage
                previousStageTimer = currentStageTimer
                currentStageTimer = -delta
                scream = SCREAM_TIME
                stageResetsIn = TRANSITION_TIME
            }
            if (scream > 0f) {
                scream -= delta
                screaming(max(0f, scream / SCREAM_TIME))
            } else if (stageResetsIn > 0f) {
                stageResetsIn = max(0f, stageResetsIn - delta)
                updatePositionDt.getOrNull(currentStage-1)?.invoke(this, previousStageTimer)
                previousPosition.set(internalPosition)
                updatePositionDt.getOrNull(currentStage)?.invoke(this, 0f)
                transitionVector.set(previousPosition).sub(internalPosition).scl(stageResetsIn / TRANSITION_TIME)
                internalPosition.add(transitionVector)
            } else {
                currentStageTimer += delta
                updatePositionDt.getOrNull(currentStage)?.invoke(this, currentStageTimer)
            }
            setPosition(internalPosition.x, internalPosition.y)
            shootingRepeater?.update(delta)
        } else {
            internalTimer -= delta
        }
    }

    val shouldBeRemoved: Boolean
        get() = !isAlive && internalTimer <= 0

    fun hit(damage: Int = 1, keepAlive: Boolean = false): Char? {
        if (lastHitSound > -1) {
            hitSound.stop(lastHitSound)
            lastHitSound = -1
        }
        hitPoints = max(0, hitPoints - damage)
        if (isAlive || keepAlive) {
            timeToMortal = invincibilityPeriod
            lastHitSound = hitSound.playSingleLow(lastHitSound, volume = 0.15f)
            val newStage = (initialHitPoints - hitPoints) / stageSize
            if (currentStage != newStage) {
                return onStageDefeat(currentStage)
            }
            return null
        }
        explodeSound.playSingleLow(volume = 0.15f)
        internalTimer = 0.5f
        texture = explosionTexture
        return onDefeat(this)
    }

    companion object {
        const val DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR = 0.5f
        const val SCREAM_TIME = 1f
        const val TRANSITION_TIME = 1f
    }

}
