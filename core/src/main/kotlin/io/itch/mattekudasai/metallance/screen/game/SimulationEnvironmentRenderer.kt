package io.itch.mattekudasai.metallance.screen.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import ktx.graphics.use

class SimulationEnvironmentRenderer : EnvironmentRenderer, Disposing by Self() {

    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer() }
    private val colors = List(2) { Color.WHITE.cpy().mul(1f / (128f - it * 100f)) }
    private val translucentColor = Color(0.1f, 0.1f, 0.1f, 0.7f)

    override fun renderBackground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2) {
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) { renderer ->
            for (i in 0..1) {
                val density = 8 - i * 3
                val netWidth = viewport.worldWidth / density
                val netHeight = viewport.worldHeight / density
                val xOffset =
                    netWidth - (((time * (i + 1).toFloat()) * 20f + flagshipPosition.x * (i + 1).toFloat() * 0.025f) % netWidth)
                val yOffset =
                    netHeight - (flagshipPosition.y * (i + 1).toFloat() * 0.125f) % netHeight
                for (j in 0..density step 2) {
                    val x = xOffset + j * netWidth
                    val y = yOffset + j * netHeight
                    renderer.color = colors[i]
                    renderer.rect(x, -1f, netWidth, viewport.worldHeight + 2f)
                    renderer.rect(-1f, y, viewport.worldWidth + 2f, netHeight)
                }
            }

        }
    }

    override fun renderForeground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2) {
        val netWidth = viewport.worldWidth * 2f
        val xOffset = netWidth - ((time * 120f) % netWidth)
        withTransparency {
            shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                it.color = translucentColor
                it.rect(xOffset - 30f, 0f, 30f, viewport.worldHeight)
            }
        }
    }
}
