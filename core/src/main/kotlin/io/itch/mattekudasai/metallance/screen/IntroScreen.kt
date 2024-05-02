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
import io.itch.mattekudasai.metallance.screen.touch.TouchMenuAdapter
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.pixel.intFloat
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.max
import kotlin.math.min

class IntroScreen(val finish: () -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)

    private val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "texture/font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = "") + ".,'0123456789:",
            fontLetterWidth = 5,
            fontLetterHeight = 9,
            fontHorizontalSpacing = 1,
            fontVerticalSpacing = 0,
            fontHorizontalPadding = 1,
        )
    }
    private val delayedTextDrawer = DelayedTextDrawer(textDrawer, ::characterTime)
    private val textures = images.map { Texture(it.overridable).autoDisposing() }
    private val music: Music by remember { Gdx.audio.newMusic("music/intro.ogg".overridable) }
    private var fadeInFor: Float = 0f
    private var fadeOutFor: Float = 0f

    private val sequence = listOf(
        1f to {}, // wait
        0f to {
            if (textIndex < textArray.size) {
                characterTime = if (textIndex == 0) 0.3f else if (textIndex == textArray.size - 1) 0.24f else 0.125f
                fadeInFor = FADE_TIME
                val text = textArray[textIndex++]
                val textDelay = text.sumOf { it.length } * characterTime + 2f
                delayedTextDrawer.startDrawing(text, viewport.worldWidth / 2f, 0f)
                actionIndex--
                currentWaitTime += textDelay
            }
        },
        1.5f to {
            fadeInFor = 0f
            fadeOutFor = FADE_TIME * 2f
        }, // wait
        1f to {
            skip()
        }
    )
    private var textIndex = 0
    private var actionIndex = 0
    private var currentWaitTime = 0f
    private val transparentColor = Color.CLEAR.cpy()
    private val touchMenuAdapter = TouchMenuAdapter(
        onDragUp = { },
        onDragDown = { },
        onTap = { skip() }
    )

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        if (actionIndex >= sequence.size) {
            return
        }

        if (delta == 0f) {
            music.pause()
        } else if (actionIndex > 0 && textIndex >= 1 && !music.isPlaying) {
            music.play()
            music.volume = 0.15f
        }

        currentWaitTime -= delta
        while (currentWaitTime < 0f && actionIndex < sequence.size) {
            val action = sequence[actionIndex++]
            action.second.invoke()
            currentWaitTime += action.first
        }

        if (fadeInFor > 0f) {
            fadeInFor = max(0f, fadeInFor - delta)
            val factor = max(0f, min(1f, 1f - fadeInFor / FADE_TIME))
            transparentColor.r = factor
            transparentColor.g = factor
            transparentColor.b = factor
            transparentColor.a = factor
        }

        if (fadeOutFor > 0f) {
            fadeOutFor = max(0f, fadeOutFor - delta)
            transparentColor.a = max(0f, min(1f, fadeOutFor / FADE_TIME))
        }

        if (actionIndex == 0) {
            return
        }

        viewport.apply(true)
        withTransparency {
            batch.use(camera) {
                if (textIndex - 1 >= 0) {
                    if (actionIndex == 1 && textIndex - 2 >= 0) { // only draw previous image if we are not fading out
                        it.color = Color.WHITE
                        it.draw(textures[textIndex - 2], 0f.intFloat, 32f.intFloat)
                    }
                    it.color = transparentColor
                    it.draw(textures[textIndex - 1], 0f.intFloat, 32f.intFloat)
                }
                it.color = if (actionIndex > 1) transparentColor else Color.WHITE
                delayedTextDrawer.updateAndDraw(delta, batch)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return touchMenuAdapter.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return touchMenuAdapter.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return touchMenuAdapter.touchUp(screenX, screenY, pointer, button)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (isPaused) {
            return false
        }
        if (keycode.isAnyKey) {
            skip()
        }
        return true
    }

    private fun skip() {
        music.stop()
        finish()
    }

    companion object {
        private var characterTime = 1f

        private val lines = """
        20XX

        Earth is invaded by alien race
        That conquered the universe

        For many years people were enslaved

        But deep underground

        They worked together to put
        everything they had

        Into creating the last hope of humanity

        the ultimate weapon

        METAL LANCE
        """.trimIndent().uppercase()

        private val textArray = lines.split("\n\n").map { it.split("\n") }

        private val images = textArray.indices.map { "texture/intro/intro$it.png" }
        private const val FADE_TIME = 0.5f
    }

}
