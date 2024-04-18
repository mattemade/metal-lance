package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isAnyKey
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.max
import kotlin.math.min

class DisclaimerScreen(val finish: () -> Unit, private val applyTint: (Color) -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

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
    private val tintColor = Color.WHITE.cpy()
    private val delayedTextDrawer = DelayedTextDrawer(textDrawer, { characterTime })

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
            tintColor.r = 1f
            applyTint(tintColor)
            finish()
        }
    )
    private var textIndex = 0
    private var actionIndex = 0
    private var currentWaitTime = 0f
    private var ignoringInputFor = 1f

    init {
    }

    var intencity = 0.2f
    var goesUp = true
    val tempColor = Color.WHITE.cpy()

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        ignoringInputFor -= delta
        if (actionIndex >= sequence.size) {
            return
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

        if (goesUp) {
            intencity = min(1f, intencity + delta * 0.5f)
            if (intencity == 1f) {
                goesUp = false
            }
        } else {
            intencity = max(0.2f, intencity - delta * 0.5f)
            if (intencity == 0.2f) {
                goesUp = true
            }
        }

        tintColor.r = intencity
        applyTint(tintColor)

        viewport.apply(true)
        batch.use(camera) {
            if (textIndex - 1 >= 0) {
                //it.draw(textures[textIndex-1], 0f, 32f)
            }
            tempColor.a = intencity
            it.color = tempColor
            delayedTextDrawer.updateAndDraw(delta, batch)
        }
        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (isPaused || ignoringInputFor > 0f) {
            return false
        }
        if (keycode.isAnyKey) {
            finish()
        }
        return true
    }

    companion object {
        private const val characterTime = 0.04f
        private val lines = """
        If you see more than two colors
        just Take a screenshot and check
        """.trimIndent().uppercase()

        private val textArray = lines.split("\n\n").map { it.split("\n") }
        //private val images = textArray.indices.map { "texture/intro/intro$it.png" }
    }

}
