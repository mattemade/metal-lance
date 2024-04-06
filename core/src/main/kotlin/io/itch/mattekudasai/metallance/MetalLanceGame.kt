package io.itch.mattekudasai.metallance

import io.itch.mattekudasai.metallance.screen.FirstScreen
import io.itch.mattekudasai.metallance.util.pixel.PixelPerfectScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MetalLanceGame : KtxGame<KtxScreen>() /* not self disposing since KtxGame disposes all the screens itself */ {
    override fun create() {
        // TODO: add start screen
        // TODO: add intro screen
        showGameScreen()
    }

    private fun showGameScreen() {
        switchToScreen(FirstScreen())
    }

    private fun switchToScreen(screen: KtxScreen) {
        removeScreen(shownScreen.javaClass)
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

