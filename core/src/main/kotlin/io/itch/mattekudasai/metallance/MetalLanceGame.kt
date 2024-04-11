package io.itch.mattekudasai.metallance

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import io.itch.mattekudasai.metallance.screen.GameScreen
import io.itch.mattekudasai.metallance.screen.IntroScreen
import io.itch.mattekudasai.metallance.screen.PausableScreen
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
            showGameScreen()
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
        switchToScreen(
            GameScreen(
                configuration = GameScreen.Configuration(
                    levelPath = "levels/tutorial.txt"
                ),
                setRenderMode = { mode, stage ->
                    getScreen<PixelPerfectScreen>().updateScreenMode(mode, stage)
                },
                setTint = { tint ->
                    getScreen<PixelPerfectScreen>().updateTint(tint)
                }
            )
        )
    }

    private fun <T> switchToScreen(screen: T) where T: KtxScreen, T: InputProcessor {
        removeScreen(shownScreen.javaClass)
        addScreen(
            PixelPerfectScreen(
                screen = PausableScreen(screen),
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

