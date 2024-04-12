package io.itch.mattekudasai.metallance.screen.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.graphics.use

class CityEnvironmentRenderer : EnvironmentRenderer, Disposing by Self() {


    private val bg0TexturePack = TexturePack (
        Texture("texture/level/city_bg0.png".overridable).autoDisposing(),
        tint = Color(0.1f, 0.1f, 0.1f, 1f),
        30f,
        0.125f,
        offsetY = 20f
    )

    private val bg1TexturePack = TexturePack (
        Texture("texture/level/city_bg1.png".overridable).autoDisposing(),
        tint = Color(0.224f, 0.224f, 0.224f, 1f),
        60f,
        0.250f,
    )

    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }

    override fun renderBackground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2) {
        bg0TexturePack.draw(viewport, camera, time, flagshipPosition)
        bg1TexturePack.draw(viewport, camera, time, flagshipPosition)
    }


    private fun TexturePack.draw(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2) {
        val xOffset = -((time + flagshipPosition.x * 0.0016f) * parallaxX % textureWidth)
        val yOffset = offsetY - (flagshipPosition.y * parallaxY) % textureHeight
        spriteBatch.use(camera) {
            it.color = tint
            it.draw(
                texture,
                xOffset,
                yOffset,
                textureWidthFloat,
                textureHeightFloat,
                0,
                0,
                textureWidth,
                textureHeight,
                false,
                false
            )
            if (textureWidth + xOffset < viewport.screenWidth) {
                it.draw(
                    texture,
                    xOffset + textureWidth,
                    yOffset,
                    textureWidthFloat,
                    textureHeightFloat,
                    0,
                    0,
                    textureWidth,
                    textureHeight,
                    false,
                    false
                )
            }
        }
    }

    private class TexturePack(
        val texture: Texture,
        val tint: Color,
        val parallaxX: Float,
        val parallaxY: Float,
        val offsetY: Float = 0f,
        val textureWidth: Int = texture.width,
        val textureHeight: Int = texture.height,
        val textureWidthFloat: Float = texture.width.toFloat(),
        val textureHeightFloat: Float = texture.height.toFloat(),
    )

    override fun renderForeground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2) {

    }
}
