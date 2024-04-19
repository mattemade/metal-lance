package io.itch.mattekudasai.metallance.`object`

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Shot(
    val initialPosition: Vector2,
    private val directionDt: ((shot: Shot, time: Float, delta: Float) -> Unit)? = null,
    texture: Texture? = null,
    private val textures: List<Pair<Texture, Float>> = texture?.let { listOf(texture to Float.MAX_VALUE) } ?: emptyList(),
    private val isRotating: Boolean = true,
    initialDirection: Vector2? = null,
    val timeToLive: Float = Float.MAX_VALUE,
) : SimpleSprite(textures[0].first) {

    val direction: Vector2 = initialDirection ?: Vector2()
    val internalPosition: Vector2 = initialPosition.cpy()
    val previousPosition = internalPosition.cpy()
    var internalTimer = 0f
        private set

    var offscreenTimeToDisappear = DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR // time to keep the shot off-screen, in case it will return back
    var currentTexture = 0
    var nextTextureIn = textures[0].second
    var markedForRemoval = false
        set(value) {
            field = value
            if (value) {
                directionDt?.invoke(this, internalTimer, 0f) // notifies associated bombs to disappear via ShootingPattern
            }
        }

    fun update(delta: Float) {
        internalTimer += delta
        directionDt?.let {
            previousPosition.set(internalPosition)
            it.invoke(this, internalTimer, delta)
        }
        internalPosition.mulAdd(direction, delta)

        nextTextureIn -= delta
        while (nextTextureIn < 0f) {
            currentTexture = (currentTexture + 1) % textures.size
            nextTextureIn += textures[currentTexture].second
            texture = textures[currentTexture].first
            val textureWidth = texture.width.toFloat()
            val textureHeight = texture.height.toFloat()
            setBounds(0f, 0f, textureWidth, textureHeight)
        }

        setPosition(internalPosition.x, internalPosition.y)
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
        const val DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR = 3f
    }

}
