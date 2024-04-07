package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.player.Shot
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.disposing.mutableDisposableListOf
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class GameScreen : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val flagship: Flagship by remember { Flagship(::shoot) }
    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shots = mutableDisposableListOf<Shot>().autoDisposing()
    private val shotTexture: Texture by remember { Texture("shot.png") }

    init {
        Gdx.input.inputProcessor = this
    }

    private fun shoot(x: Float, y: Float) {
        shots += Shot(x, y, shotTexture)
    }

    override fun render(delta: Float) {
        clearScreen(red = 0f, green = 0f, blue = 0f)

        flagship.update(delta)
        shots.removeAll { it.update(delta) > viewport.worldWidth }

        viewport.apply(true)
        batch.use(camera) { batch ->
            shots.forEach { it.draw(batch) }
            flagship.draw(batch)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    override fun keyDown(keycode: Int): Boolean {
        return flagship.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return flagship.keyUp(keycode)
    }
}
