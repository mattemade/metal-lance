package io.itch.mattekudasai.metallance.util.pixel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.min

// TODO: think over these val width/height, they may need to become vars at some point with widescreen upgrade
class PixelPerfectScreen(private val screen: KtxScreen, private val virtualWidth: Float, private val virtualHeight: Float) : KtxScreen, Disposing by Self() {

    private val origShaderProgram: ShaderProgram by remember { createDefaultShader() }
    private val origSpriteBatch: SpriteBatch by remember { SpriteBatch(1000, origShaderProgram) }
    private val frameBuffer: FrameBuffer by remember {
        FrameBuffer(
            Pixmap.Format.RGBA8888,
            virtualWidth.toInt(),
            virtualHeight.toInt(),
            false,
            false
        )
    }
    private val origViewport: Viewport = FitViewport(virtualWidth, virtualHeight)
    private val frameBufferRegion: TextureRegion =
        TextureRegion(
            frameBuffer.colorBufferTexture.apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            }
        ).apply { flip(false, true) }

    init {
        screen.autoDisposing()
    }

    override fun render(delta: Float) {
        frameBuffer.use {
            screen.render(delta)
        }
        origViewport.apply(true)
        origSpriteBatch.use(origViewport.camera) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            it.draw(frameBufferRegion, 0f, 0f, virtualWidth, virtualHeight)
        }
    }

    override fun pause() {
        screen.pause()
    }

    override fun resume() {
        screen.resume()
    }

    override fun show() {
        screen.show()
    }

    override fun resize(width: Int, height: Int) {
        val fitsByWidth = (width / virtualWidth).toInt()
        val fitsByHeight = (height / virtualHeight).toInt()
        val minFits = min(fitsByWidth, fitsByHeight)
        val viewportScreenWidth = (virtualWidth * minFits).toInt()
        val viewportScreenHeight = (virtualHeight * minFits).toInt()

        origViewport.update(
            viewportScreenWidth,
            viewportScreenHeight
        )
        origViewport.screenX = (width - viewportScreenWidth) / 2
        origViewport.screenY = (height - viewportScreenHeight) / 2

        origShaderProgram.use {
          // TODO update shader program
        }

        // TODO: maybe call that in create() only once? but maybe not, since virtual size may change later
        screen.resize(virtualWidth.toInt(), virtualHeight.toInt())
    }

    // TODO: do a cool shader for cool effects
    private fun createDefaultShader(): ShaderProgram =
        SpriteBatch.createDefaultShader()
}
