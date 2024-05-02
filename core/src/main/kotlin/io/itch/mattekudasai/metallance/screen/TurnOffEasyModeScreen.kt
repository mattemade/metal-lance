package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isDown
import io.itch.mattekudasai.metallance.player.Controls.isShoot
import io.itch.mattekudasai.metallance.player.Controls.isUp
import io.itch.mattekudasai.metallance.screen.touch.TouchMenuAdapter
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class TurnOffEasyModeScreen(
    val apply: (easyMode: Boolean) -> Unit,
) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shipTexture: Texture by remember { Texture("texture/ship/normal.png".overridable) }
    private var internalTimer = 0f
    private var selection = 0
    private val menuItems = listOf("TURN OFF", "KEEP EASY")

    private val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "texture/font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = "") + ".,'0123456789:Ж?",
            fontLetterWidth = 5,
            fontLetterHeight = 9,
            fontHorizontalSpacing = 1,
            fontVerticalSpacing = 0,
            fontHorizontalPadding = 1,
        )
    }
    private val touchMenuAdapter = TouchMenuAdapter(
        onDragUp = { if (selection > 0) moveCursorUp() },
        onDragDown = { if (selection < menuItems.size - 1) moveCursorDown() },
        onTap = { select() }
    )

    override fun render(delta: Float) {
        internalTimer += delta
        clearScreen(red = 0f, green = 0f, blue = 0f)

        viewport.apply(true)
        batch.use(camera) {
            textDrawer.drawText(
                it,
                listOf("TURN OFF EASY MODE?"),
                viewport.worldWidth / 2f,
                viewport.worldHeight * 0.8f,
                Align.top
            )
            val menuGoesFrom = viewport.worldHeight / 3f
            menuItems.forEachIndexed { index, item ->
                val itemY = menuGoesFrom - index * 32f
                textDrawer.drawText(it, listOf(item), viewport.worldWidth / 2f, itemY, Align.top)
                if (selection == index) {
                    it.draw(
                        shipTexture,
                        viewport.worldWidth * 0.27f,
                        itemY + 11f,
                        shipTexture.width.toFloat(),
                        shipTexture.height.toFloat()
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
        if (keycode.isShoot || keycode == Keys.SPACE || keycode == Keys.ENTER) {
            select()
        } else if (keycode.isUp) {
            moveCursorUp()
        } else if (keycode.isDown) {
            moveCursorDown()
        }
        return true
    }

    private fun select() {
        when (selection) {
            0 -> apply(false)
            1 -> apply(true)
        }
    }

    private fun moveCursorUp() {
        selection = (selection + 1) % menuItems.size
    }

    private fun moveCursorDown() {
        selection = (selection + menuItems.size - 1) % menuItems.size
    }

}
