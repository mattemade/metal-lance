package io.itch.mattekudasai.metallance.`object`

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import ktx.graphics.use
import kotlin.math.min

class Bomb(
    val internalPosition: Vector2,
    private val speed: Float,
    private val maxRadius: Float,
    val isEnemy: Boolean,
    private val stayFor: Float,
    private val fadeOutIn: Float,
    val outerColor: Color,
    val innerColor: Color? = null,
    private val shapeRenderer: ShapeRenderer,
    private val progress: (Float) -> Unit = { },
): Disposing by Self() {

    private var internalTime = 0f
    private var finishesIn = maxRadius / speed
    private var currentRadius = 0f

    private val hittedObjects = mutableSetOf<Any>()
    private val innerColorMod = innerColor?.cpy()
    private val outerColorMod = outerColor.cpy()

    fun update(delta: Float): Boolean {
        internalTime += delta
        currentRadius = min(maxRadius, internalTime*speed)
        if (internalTime > finishesIn + stayFor) {
            val currentFadeOut = min(internalTime - finishesIn - stayFor, fadeOutIn)
            val factor = 1f - currentFadeOut / fadeOutIn
            // TODO: fix the problem of alpha lower than 0.127 to be visible
            innerColorMod?.a = (innerColorMod?.a ?: 0f) * factor
            outerColorMod.a = outerColor.a * factor
            progress(factor)
        } else if (internalTime >= finishesIn + stayFor + fadeOutIn) {
            progress(0f)
        } else {
            progress(1f)
        }

        val result = internalTime < finishesIn + stayFor + fadeOutIn
        return result
    }

    fun hits(position: Vector2, distance: Float): Boolean =
        internalPosition.dst(position) < currentRadius + distance
    fun hits(position: Vector2, distance: Float, objectToRemember: Any): Boolean =
        (hits(position, distance) && !hittedObjects.contains(objectToRemember)).also {
            if (it) {
                hittedObjects += objectToRemember
            }
        }

    fun draw(camera: Camera) = with(shapeRenderer) {
        withTransparency {
            if (innerColorMod != null) {
                shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                    color = innerColorMod
                    circle(internalPosition.x, internalPosition.y, currentRadius)
                }
            }
            shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
                color = outerColorMod
                circle(internalPosition.x, internalPosition.y, currentRadius)
            }
        }
    }

}
