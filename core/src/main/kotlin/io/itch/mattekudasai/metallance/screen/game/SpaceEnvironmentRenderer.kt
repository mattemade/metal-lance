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

class SpaceEnvironmentRenderer : EnvironmentRenderer, Disposing by Self() {


    private val earthTexturePack = TexturePack (
        Texture("texture/level/earth_bg_2.png".overridable).autoDisposing(),
        tint = Color(0.8f, 0.8f, 0.8f, 0.5f),
        0.5f,
        0.25f,
        offsetY = 8f
    )

    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }

    override fun renderBackground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2?) {
        earthTexturePack.draw(viewport, camera, time, flagshipPosition)
    }

    private var lastKnownFlagshipX = 0f
    private var lastKnownFlagshipY = 0f

    private fun TexturePack.draw(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2?) {
        flagshipPosition?.let {
            lastKnownFlagshipX = it.x
            lastKnownFlagshipY = it.y
        }
        val xOffset = - (lastKnownFlagshipX * parallaxX) % textureWidth
        val yOffset = offsetY - (lastKnownFlagshipY * parallaxY)
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

    override fun renderForeground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2?) {

    }
}
