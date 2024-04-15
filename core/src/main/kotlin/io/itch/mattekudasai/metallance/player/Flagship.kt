package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.`object`.Shot
import io.itch.mattekudasai.metallance.player.Controls.isBackward
import io.itch.mattekudasai.metallance.player.Controls.isDown
import io.itch.mattekudasai.metallance.player.Controls.isForward
import io.itch.mattekudasai.metallance.player.Controls.isShoot
import io.itch.mattekudasai.metallance.player.Controls.isSlow
import io.itch.mattekudasai.metallance.player.Controls.isUp
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.sound.playSingleLow
import java.lang.Math.pow
import ktx.app.KtxInputAdapter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// TODO: think about dynamic world width/height
class Flagship(
    private val worldWidth: Float,
    private val worldHeight: Float,
    private val hudHeight: Float,
    private val easyMode: Boolean,
    private val explosionTexture: Texture,
    private val initialLivesLeft: Int,
    private val initialPower: Int,
    private val initialCharge: Int,
    private val initialShipType: Int,
    private val shot: (shipType: Int) -> Unit,
    private val tempoProvider: () -> Float,
) : SimpleSprite("texture/ship/normal.png"), KtxInputAdapter {

    private val state = State(40f, worldHeight / 2f)
    val internalPosition: Vector2 get() = state.position
    val rearPosition = Vector2()
    val frontPosition = Vector2()
    private val shipTextures = listOf(
        texture,
        Texture("texture/ship/double.png".overridable).autoDisposing(),
        Texture("texture/ship/triple.png".overridable).autoDisposing(),
        Texture("texture/ship/quadriple.png".overridable).autoDisposing(),
        Texture("texture/ship/quintiple.png".overridable).autoDisposing(),
    )
    var shipType: Int = 0
        private set(value) {
            val constrainedValue = min(shipTextures.size - 1, value)
            texture = shipTextures[constrainedValue]
            val textureWidth = texture.width.toFloat()
            val textureHeight = texture.height.toFloat()
            setBounds(0f, 0f, textureWidth, textureHeight)
            setPosition(
                state.position.x - textureWidth/2f,
                state.position.y - textureHeight/2f
            )
            field = constrainedValue
        }

    var lives = initialLivesLeft
    var power: Int = initialPower // 0 to 5
        private set

    var charge: Int = initialCharge
        private set
    private val shootingCooldown: Float
        get() = 60f / tempoProvider() / 2f.pow((power + 1)/2)
    var isAlive = true
      private set

    val isInvincible: Boolean
        get() = isAlive && timeToStartOver > 0f

    var slowingTransition: Float = 0f

    private val hitSound: Sound by remember { Gdx.audio.newSound("sound/player_hit.ogg".overridable) }
    private val explodeSound: Sound by remember { Gdx.audio.newSound("sound/explosion.ogg".overridable) }
    private val shotSound: Sound by remember { Gdx.audio.newSound("sound/player_shot.ogg".overridable) }
    private val powerUpSound: Sound by remember { Gdx.audio.newSound("sound/power_up.ogg".overridable) }
    private val upgradeSound: Sound by remember { Gdx.audio.newSound("sound/upgrade_ship.ogg".overridable) }
    private val fullPowerSound: Sound by remember { Gdx.audio.newSound("sound/full_power.ogg".overridable) }
    private val beyondFullPowerSound: Sound by remember { Gdx.audio.newSound("sound/beyond_full_power.ogg".overridable) }
    init {
        shipType = initialShipType
    }

    override fun keyDown(keycode: Int): Boolean {
        with(state) {
            when {
                keycode.isForward -> flyingForward = true
                keycode.isBackward -> flyingBackward = true
                keycode.isUp -> flyingUp = true
                keycode.isDown -> flyingDown = true
                keycode.isShoot -> shooting = true
                keycode.isSlow -> slow = true
                Gdx.app.logLevel == Application.LOG_DEBUG && keycode == Input.Keys.T -> transform()
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
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        if (!isAlive) {
            movingTo.set(-Shot.SPEED_SLOW*delta, 0f)
            state.position.add(movingTo)
            setPosition(
                (state.position.x - halfWidth).toInt().toFloat(),
                (state.position.y - halfHeight).toInt().toFloat()
            )
            return
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
            if (slowingTransition < 1f) {
                slowingTransition = min(1f, slowingTransition + delta / SLOWING_TRANSITION_TIME)
            }
        } else if (slowingTransition > 0f) {
            slowingTransition = max(0f, slowingTransition - delta / SLOWING_TRANSITION_TIME)
        }


        with(state.position) {
            add(movingTo)
            if (x < halfWidth) {
                x = halfWidth
            } else if (x > worldWidth - halfWidth) {
                x = worldWidth - halfWidth
            }
            if (y < hudHeight + halfHeight) {
                y = hudHeight + halfHeight
            } else if (y > worldHeight - halfHeight) {
                y = worldHeight - halfHeight
            }
        }
        setPosition(
            (state.position.x - halfWidth).toInt().toFloat(),
            (state.position.y - halfHeight).toInt().toFloat()
        )

        if (state.timeFromLastShot > shootingCooldown && state.shooting) {
            state.timeFromLastShot = 0f
            shotSound.playSingleLow(volume = 1f / 2f.pow(power * 0.3f + 2f))
            shot(shipType)
        }
        state.timeFromLastShot += delta
        rearPosition.set(state.position).sub(3f, 0f)
        frontPosition.set(state.position).add(3f, 0f)
    }

    fun powerUp() {
        playPowerUpSound(power)
        power = min(5, power + 1)
    }

    fun chargeUp() {
        playPowerUpSound(charge)
        charge = min(5, charge + 1)
    }

    private fun playPowerUpSound(state: Int) =
        when (state) {
            5 -> beyondFullPowerSound
            4 -> fullPowerSound
            else -> powerUpSound
        }.playSingleLow(volume = 0.15f)

    fun transform() {
        upgradeSound.playSingleLow(volume = 0.2f)
        val oldShipType = shipType++
        if (!easyMode && oldShipType != shipType) {
            power = 0
        }
    }

    // TODO: maybe reuse is for game over?
    private fun explode() {
        explodeSound.playSingleLow(volume = 0.2f)
        isAlive = false
        texture = explosionTexture
        setBounds(0f, 0f, explosionTexture.width.toFloat(), explosionTexture.height.toFloat())
        setPosition(state.position.x - explosionTexture.width / 2, state.position.y - explosionTexture.height / 2)
        timeToStartOver = 1f
    }

    fun startOver(): Boolean {
        if (!easyMode) {
            if (--lives < 0) {
                explode()
                return false
            }
        }
        hitSound.playSingleLow()
        isAlive = true
        if (!easyMode) {
            shipType = 0
            power = 0
            charge = 0
        }
        timeToStartOver = 2f
        return true
    }

    companion object {
        const val SPEED_LIMIT = 200f
        const val HORIZONTAL_SPEED = SPEED_LIMIT
        const val VERTICAL_SPEED = SPEED_LIMIT * 0.66f
        const val DIAGONAL_SPEED_FACTOR = SPEED_LIMIT / (HORIZONTAL_SPEED + VERTICAL_SPEED)
        const val SLOWING_FACTOR = 0.5f
        const val SHOOTING_COOLDOWN = 0.5f
        const val SLOWING_TRANSITION_TIME = 0.125f

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
