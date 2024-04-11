package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
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

class TitleScreen(
    val startTutorial: () -> Unit,
    val startGame: () -> Unit
) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val metalTexture: Texture by remember { Texture("texture/title/metal.png".overridable) }
    private val lanceTexture: Texture by remember { Texture("texture/title/lance.png".overridable) }
    private val weaponTexture: Texture by remember { Texture("texture/title/weapon.png".overridable) }
    private val shipTexture: Texture by remember { Texture("texture/ship/normal.png".overridable) }
    private val music: Music by remember { Gdx.audio.newMusic("music/title.ogg".overridable) }
    private var musicShouldBeResumed = false
    private var internalTimer = 0f
    private var selection = 1
    private val menuItems = listOf("TUTORIAL", "START GAME")

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
        music.isLooping = true
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
            it.draw(weaponTexture, (viewport.worldWidth - weaponTexture.width) / 2f, viewport.worldHeight * 0.75f)
            it.draw(lanceTexture, (viewport.worldWidth - lanceTexture.width) / 2f, viewport.worldHeight * 0.75f)
            it.draw(metalTexture, (viewport.worldWidth - metalTexture.width) / 2f, viewport.worldHeight * 0.75f)
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
        if (keycode.isShoot || keycode == Keys.SPACE || keycode == Keys.ENTER) {
            music.stop()
            when (selection) {
                0 -> startTutorial()
                1 -> startGame()
            }
        } else if (keycode.isUp) {
            selection = (selection + 1) % menuItems.size
        } else if (keycode.isDown) {
            selection = (selection + menuItems.size - 1) % menuItems.size
        }
        return true
    }

}
