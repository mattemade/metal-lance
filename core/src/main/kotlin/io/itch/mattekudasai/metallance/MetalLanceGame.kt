package io.itch.mattekudasai.metallance

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import io.itch.mattekudasai.metallance.screen.AutoPausingScreen
import io.itch.mattekudasai.metallance.screen.GameOverScreen
import io.itch.mattekudasai.metallance.screen.GameScreen
import io.itch.mattekudasai.metallance.screen.IntroScreen
import io.itch.mattekudasai.metallance.screen.OutroScreen
import io.itch.mattekudasai.metallance.screen.TitleScreen
import io.itch.mattekudasai.metallance.util.pixel.PixelPerfectScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen

class MetalLanceGame : KtxGame<KtxScreen>() /* not self disposing since KtxGame disposes all the screens itself */ {

    override fun create() {
        // TODO: set to LOG_NONE
        Gdx.app.logLevel = Application.LOG_NONE
        // TODO: showIntro() instead
        if (false) {
            showGameScreen(
                GameScreen.Configuration(
                    levelPath = "levels/stage1.txt"
                )
            )
        } else {
            showIntro()
        }
    }

    private fun showIntro() {
        switchToScreen(IntroScreen { showTitle() })
    }

    private fun showTitle() {
        switchToScreen(TitleScreen(
            startTutorial = {
                showGameScreen(
                    GameScreen.Configuration(
                        levelPath = "levels/tutorial.txt",
                        livesLeft = Int.MAX_VALUE
                    )
                )
            },
            startGame = {
                showGameScreen(
                    GameScreen.Configuration(
                        levelPath = "levels/stage1.txt"
                    )
                )
            }
        ))
    }

    private fun showGameScreen(configuration: GameScreen.Configuration) {
        switchToScreen(
            GameScreen(
                configuration = configuration,
                setRenderMode = { mode, stage ->
                    getScreen<PixelPerfectScreen>().updateScreenMode(mode, stage)
                },
                setTint = { tint ->
                    getScreen<PixelPerfectScreen>().updateTint(tint)
                },
                returnToMainMenu = { showTitle() },
                showGameOver = {
                    showGameOver(it)
                },
                advance = {
                    when (it.levelPath) {
                        "levels/stage1.txt" -> showGameScreen(it.copy(levelPath = "levels/stage2.txt"))
                        "levels/stage2.txt" -> showGameScreen(it.copy(levelPath = "levels/stage3.txt"))
                        "levels/stage3.txt" -> showOutro()
                    }
                },
                restart = {
                    showGameScreen(configuration)
                }
            )
        )
    }

    private fun showGameOver(configuration: GameScreen.Configuration) {
        switchToScreen(GameOverScreen(
            continueGame = {
                showGameScreen(
                    GameScreen.Configuration(
                        levelPath = configuration.levelPath,
                    )
                )
            },
            showTitle = { showTitle() }
        ))
    }

    private fun showOutro() {
        switchToScreen(OutroScreen { showTitle() })
    }

    private fun <T> switchToScreen(screen: T) where T : KtxScreen, T : InputProcessor {
        removeScreen(shownScreen.javaClass)
        addScreen(
            PixelPerfectScreen(
                screen = AutoPausingScreen(screen),
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

