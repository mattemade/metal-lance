package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.assets.loaders.MusicLoader
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class TitleScreen(val startGame: () -> Unit) : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val metalTexture: Texture by remember { Texture("title/metal.png".overridable) }
    private val lanceTexture: Texture by remember { Texture("title/lance.png".overridable) }
    private val weaponTexture: Texture by remember { Texture("title/weapon.png".overridable) }
    private val music: Music by remember { Gdx.audio.newMusic("music/title.ogg".overridable) }

    private val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = "") + ".,'0123456789:",
            fontLetterWidth = 5,
            fontLetterHeight = 9,
            fontHorizontalSpacing = 1,
            fontVerticalSpacing = 0,
            fontHorizontalPadding = 1,
        )
    }

    init {
        Gdx.input.inputProcessor = this
        music.play()
        music.isLooping = true
    }
    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)
        viewport.apply(true)
        batch.use(camera) {
            it.draw(weaponTexture, (viewport.worldWidth - weaponTexture.width) / 2f, viewport.worldHeight * 0.75f)
            it.draw(lanceTexture, (viewport.worldWidth - lanceTexture.width) / 2f, viewport.worldHeight * 0.75f)
            it.draw(metalTexture, (viewport.worldWidth - metalTexture.width) / 2f, viewport.worldHeight * 0.75f)
            textDrawer.drawText(it, listOf("PRESS ANY KEY TO START"), viewport.worldWidth/2f, viewport.worldHeight/4f, Align.top)
        }
        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Keys.SPACE) {
            music.stop()
            startGame()
        }
        return true
    }

}
