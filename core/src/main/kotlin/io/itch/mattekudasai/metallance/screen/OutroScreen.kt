package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isAnyKey
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.min

class OutroScreen(val finish: () -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val music: Music by remember { Gdx.audio.newMusic("music/outro.ogg".overridable) }

    private var textIndex = 0
    private var currentFrameIndex = 0
    private var internalTimer = 0f
    private var currentPartIndex = 0
    private var opacity = 0f
    private var fadingOut = false
    private var fadingIn = false
    private val images = (1..5).map { Texture("texture/outro/outro$it.png").autoDisposing() }
    private val tempColor = Color.WHITE.cpy()

    init {
        music.play()
        music.volume = 0.1f
    }

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        if (delta == 0f) {
            music.pause()
        } else if (!music.isPlaying && internalTimer < 25f) {
            music.play()
        }

        internalTimer += delta

        if (internalTimer > 0f) {
            val currentFrameIndex = min(4, (internalTimer / fourFramesIn).toInt())
            val timeInFrame = internalTimer % fourFramesIn
            val currentPanel = min(if (currentFrameIndex == 4) 0 else 3, (timeInFrame / partTime).toInt())
            val currentOpacity = if (internalTimer >= fourFramesIn * 5f) {
                1f
            } else if (currentPanel < 3 && timeInFrame < currentPanel * partTime + fadeTime) {
                (timeInFrame - currentPanel * partTime) / fadeTime
            } else if (currentFrameIndex < 4 && timeInFrame > (fourFramesIn - fadeTime)) {
                1f - (timeInFrame - (fourFramesIn - fadeTime)) / fadeTime
            } else {
                1f
            }

            viewport.apply(true)
            batch.use(camera) {
                tempColor.a = if (currentPanel < 3) 1f else currentOpacity
                it.color = tempColor
                if (currentFrameIndex < 4 && currentPanel > 0) {
                    for (i in 0..<currentPanel) {
                        val xInt = 256 / 3 * i
                        it.draw(images[currentFrameIndex], xInt.toFloat(), 0f, xInt, 0, 256 / 3, 240)
                    }
                }/*tempColor.r = currentOpacity
                tempColor.g = currentOpacity
                tempColor.b = currentOpacity*/
                tempColor.a = currentOpacity
                it.color = tempColor
                if (currentFrameIndex == 4) {
                    it.draw(images[currentFrameIndex], 0f, 0f)
                } else {
                    val xInt = 256 / 3 * currentPanel
                    it.draw(
                        images[currentFrameIndex], xInt.toFloat(), 0f, xInt, 0, 256 / 3, 240
                    )
                }
            }

        }


        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (isPaused || currentPartIndex > 0f) {
            return false
        }
        if (keycode.isAnyKey) {
            music.stop()
            finish()
        }
        return true
    }

    companion object {
        private const val fadeTime = 0.125f
        private const val fourFramesIn = 4f
        private const val partTime = fourFramesIn / 4f
    }

}
