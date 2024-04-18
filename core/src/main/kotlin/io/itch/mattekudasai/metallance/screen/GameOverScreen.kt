package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.GlobalState.isPaused
import io.itch.mattekudasai.metallance.player.Controls.isDown
import io.itch.mattekudasai.metallance.player.Controls.isShoot
import io.itch.mattekudasai.metallance.player.Controls.isUp
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class GameOverScreen(
    val showEasyMode: Boolean,
    val continueGame: (easyMode: Boolean) -> Unit,
    val showTitle: () -> Unit
) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shipTexture: Texture by remember { Texture("texture/ship/normal.png".overridable) }
    private val music: Music by remember { Gdx.audio.newMusic("music/gameover.ogg".overridable) }
    private var musicShouldBeResumed = false
    private var internalTimer = 0f
    private var selection = if (showEasyMode) 1 else 0
    private val menuItems = listOf("EASY MODE".takeIf { showEasyMode }, "CONTINUE", "MAIN MENU").filterNotNull()

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

    init {
        music.play()
        music.volume = 0.1f
    }
    override fun render(delta: Float) {
        internalTimer += delta
        clearScreen(red = 0f, green = 0f, blue = 0f)

        if (delta == 0f) {
            if (music.isPlaying) {
                music.pause()
                musicShouldBeResumed = true
            }
        } else if (musicShouldBeResumed) {
            music.play()
            musicShouldBeResumed = false
        }

        viewport.apply(true)
        batch.use(camera) {
            textDrawer.drawText(it, listOf("GAME OVER"), viewport.worldWidth / 2f, viewport.worldHeight * 0.8f, Align.top)
            val menuGoesFrom = viewport.worldHeight / 3f
            menuItems.forEachIndexed { index, item ->
                val itemY = menuGoesFrom - index * 32f
                textDrawer.drawText(it, listOf(item), viewport.worldWidth/2f, itemY, Align.top)
                if (selection == index) {
                    it.draw(shipTexture, viewport.worldWidth * 0.27f, itemY + 11f, shipTexture.width.toFloat(), shipTexture.height.toFloat())
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
        if (isPaused) {
            return false
        }
        if (keycode.isShoot || keycode == Keys.SPACE || keycode == Keys.ENTER) {
            music.stop()
            when (selection) {
                0 -> continueGame(showEasyMode)
                1 -> if (showEasyMode) continueGame(false) else showTitle()
                2 ->  showTitle()
            }
        } else if (keycode.isUp) {
            selection = (selection + menuItems.size - 1) % menuItems.size
        } else if (keycode.isDown) {
            selection = (selection + 1) % menuItems.size
        }
        return true
    }

}
