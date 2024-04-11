package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isAnyKey
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class OutroScreen(val finish: () -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

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
    private val delayedTextDrawer = DelayedTextDrawer(textDrawer, characterTime)
    //private val textures = images.map { Texture(it.overridable).autoDisposing() }
    private val music: Music by remember { Gdx.audio.newMusic("music/intro.ogg".overridable) }

    private val sequence = listOf(
        0.5f to {}, // wait
        0f to {
            if (textIndex < textArray.size) {
                val text = textArray[textIndex++]
                val textDelay = text.sumOf { it.length } * characterTime + 2f
                delayedTextDrawer.startDrawing(text, viewport.worldWidth / 2f, viewport.worldHeight / 2f)
                actionIndex--
                currentWaitTime += textDelay
            }
        },
        1f to { }, // wait
        0f to {
            music.stop()
            finish()
        }
    )
    private var textIndex = 0
    private var actionIndex = 0
    private var currentWaitTime = 0f

    init {
        music.play()
        music.isLooping = true
    }

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        if (actionIndex >= sequence.size) {
            return
        }

        if (delta == 0f) {
            music.pause()
        } else if (!music.isPlaying) {
            music.play()
        }

        currentWaitTime -= delta
        while (currentWaitTime < 0f && actionIndex < sequence.size) {
            val action = sequence[actionIndex++]
            action.second.invoke()
            currentWaitTime += action.first
        }

        if (actionIndex == 0) {
            return
        }

        viewport.apply(true)
        batch.use(camera) {
            if (textIndex-1 >= 0) {
                //it.draw(textures[textIndex-1], 0f, 32f)
            }
            delayedTextDrawer.updateAndDraw(delta, batch)
        }
        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (isPaused) {
            return false
        }
        if (keycode.isAnyKey) {
            music.stop()
            finish()
        }
        return true
    }

    companion object {
        private const val characterTime = 0.125f
        private val lines = """
        Congratulations

        You have completed this
        incomplete game

        Come back later once
        it will be completed
        """.trimIndent().uppercase()

        private val textArray = lines.split("\n\n").map { it.split("\n") }
        //private val images = textArray.indices.map { "texture/intro/intro$it.png" }
    }

}
