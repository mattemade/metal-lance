package io.itch.mattekudasai.metallance

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import io.itch.mattekudasai.metallance.screen.GameScreen
import io.itch.mattekudasai.metallance.screen.IntroScreen
import io.itch.mattekudasai.metallance.screen.TitleScreen
import io.itch.mattekudasai.metallance.util.pixel.PixelPerfectScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MetalLanceGame : KtxGame<KtxScreen>() /* not self disposing since KtxGame disposes all the screens itself */ {
    override fun create() {
        // TODO: switch to LOG_NONE
        Gdx.app.logLevel = Application.LOG_DEBUG
        // TODO: showIntro() instead
        if (true) {
            switchToScreen(GameScreen(playMusic = false))
        } else {
            showIntro()
        }
    }

    private fun showIntro() {
        switchToScreen(IntroScreen { showTitle() })
    }

    private fun showTitle() {
        switchToScreen(TitleScreen { showGameScreen() })
    }

    private fun showGameScreen() {
        switchToScreen(GameScreen())
    }

    private fun switchToScreen(screen: KtxScreen) {
        removeScreen(shownScreen.javaClass)//?.disposeSafely()
        addScreen(
            PixelPerfectScreen(
                screen = screen,
                virtualWidth = VIRTUAL_WIDTH,
                virtualHeight = VIRTUAL_HEIGHT
            )
        )
        setScreen<PixelPerfectScreen>()
    }

    companion object {
        const val VIRTUAL_WIDTH = 256f
        const val VIRTUAL_HEIGHT = 240f
    }
}

