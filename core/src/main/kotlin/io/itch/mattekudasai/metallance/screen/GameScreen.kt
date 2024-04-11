package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.enemy.Enemy
import io.itch.mattekudasai.metallance.enemy.ShootingPattern.Companion.toPattern
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.player.Shot
import io.itch.mattekudasai.metallance.screen.game.CityEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.EnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.NoopEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.SimulationEnvironmentRenderer
import io.itch.mattekudasai.metallance.stage.Level
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.disposing.mutableDisposableListOf
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.pixel.intFloat
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use

class GameScreen(
    private val configuration: Configuration,
    private val setRenderMode: (mode: Int, stage: Int) -> Unit,
    private val setTint: (Color) -> Unit,
    private val returnToMainMenu: () -> Unit,
    private val showGameOver: (configuration: Configuration) -> Unit,
    private val advance: (configuration: Configuration) -> Unit,
) : KtxScreen,
    KtxInputAdapter, Disposing by Self() {

    private val flagship: Flagship by remember {
        Flagship(
            viewport.worldWidth,
            viewport.worldHeight,
            explosionTexture,
            configuration.livesLeft,
            configuration.power,
            configuration.shipType,
            ::shoot
        )
    }
    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val enemies = mutableDisposableListOf<Enemy>(onDisposed = ::forget).autoDisposing()
    private val enemyShots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val powerUps = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val bombs = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()

    // TODO: pack all the textures in one 2048x2048 atlas to avoid constant rebinding
    private val shotTexture: Texture by remember { Texture("texture/bullet/shot.png".overridable) }
    private val enemyTextures = listOf("texture/saucer.png").map {
        Texture(it.overridable).autoDisposing()
    }
    private val explosionTexture: Texture by remember { Texture("texture/explosion.png".overridable) }
    private val enemyShotTextures = listOf("texture/bullet/wave.png").map {
        Texture(it.overridable).autoDisposing()
    }
    //private val powerUpTexture: Texture by remember { Texture("texture/upgrade/power.png".overridable) }
    private val powerUpTextures = (0..2).map { pIndex ->
        (0..9).map { frame ->
            Texture("texture/upgrade/animated/p${pIndex}_$frame.png".overridable).autoDisposing() to if (frame == 9) 0.292f else 0.042f
        }
    }
    private var currentPTexture = 0
    private var environmentRenderer: EnvironmentRenderer = NoopEnvironmentRenderer
    private var defeatToWin: Int = Integer.MAX_VALUE
    private var gameOverTimer: Float = 0f

    private val level = Level(
        scriptFile = configuration.levelPath.overridable,
        setBackground = {
            environmentRenderer = when (it) {
                "simulation" -> environmentRenderer as? SimulationEnvironmentRenderer
                    ?: SimulationEnvironmentRenderer().autoDisposing()

                "city" -> environmentRenderer as? CityEnvironmentRenderer ?: CityEnvironmentRenderer().autoDisposing()

                else -> NoopEnvironmentRenderer
            }
        },
        showText = {
            delayedTextDrawer.startDrawing(
                it.text,
                it.positionX * viewport.worldWidth,
                it.positionY * viewport.worldHeight
            )
        },
        spawnEnemy = { enemyConfiguration ->
            val initialPosition = Vector2()
            if (Align.isRight(enemyConfiguration.alignment)) {
                initialPosition.set(viewport.worldWidth + 8f, enemyConfiguration.alignmentFactor * viewport.worldHeight)
            }
            val pattern = enemyConfiguration.shootingPattern.toPattern()
            enemies += Enemy(
                texture = enemyTextures[enemyConfiguration.enemyType],
                explosionTexture = explosionTexture,
                initialPosition = initialPosition,
                updatePositionDt = enemyConfiguration.updatePositionDt,
                initialShootingDelay = pattern.initialDelay,
                nextShootingDelay = pattern.nextDelay,
                shot = { enemyShots += pattern.onShoot(it, flagship.internalPosition, enemyShotTextures[pattern.shotTextureIndex]) }
            )
        },
        winningCondition = { condition, counter ->
            when (condition) {
                "kill" -> defeatToWin = counter
            }
        },
        endSequence = {
            when (configuration.sequenceEndAction) {
                EndAction.RETURN_TO_MENU -> {
                    music?.stop()
                    returnToMainMenu()
                }
            }
        },
        setRenderMode = setRenderMode,
        setTint = setTint,
        playMusic = { path, volume ->
            music?.stop()
            music = null
            val file = path.overridable
            if (file.exists()) {
                music = Gdx.audio.newMusic(file).autoDisposing().also {
                    it.play()
                    it.volume = volume
                    it.isLooping = true
                }
            }
        }
    )

    val textDrawer: MonoSpaceTextDrawer by remember {
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
    val delayedTextDrawer = DelayedTextDrawer(textDrawer, 0.125f)
    private var music: Music? = null

    private fun spawnShot(offsetX: Float = 0f, offsetY: Float = 0f, angleDeg: Float = 0f): Shot =
        Shot(
            internalPosition = flagship.internalPosition.cpy().add(offsetX, offsetY),
            initialDirection = Vector2(Shot.SPEED_FAST, 0f).rotateDeg(angleDeg),
            texture = shotTexture
        ).also { shots += it }


    private fun shoot(shipType: Int) {
        val bigAngle = (1f - flagship.slowingTransition) * 45
        val shortAngle = (1f - flagship.slowingTransition) * 30
        val slowingOffset = flagship.slowingTransition * 5f
        when (shipType) {
            0 -> spawnShot()
            1 -> {
                spawnShot(offsetY = 3f)
                spawnShot(offsetY = -3f)
            }

            2 -> {
                spawnShot(-2f + slowingOffset, slowingOffset, -bigAngle)
                spawnShot()
                spawnShot(-2f + slowingOffset, -slowingOffset, bigAngle)
            }

            3 -> {
                spawnShot(-2f, -2f - slowingOffset, -bigAngle)
                spawnShot(1f, -slowingOffset / 2f, -shortAngle)
                spawnShot(-2f, 2f + slowingOffset, bigAngle)
                spawnShot(1f, slowingOffset / 2f, shortAngle)
            }

            4 -> {
                spawnShot(-5f, -2f - slowingOffset, -bigAngle)
                spawnShot(-2f, -slowingOffset / 2f, -shortAngle)
                spawnShot()
                spawnShot(-5f, 2f + slowingOffset, bigAngle)
                spawnShot(-2f, slowingOffset / 2f, shortAngle)
            }
        }
    }

    private fun spawnPowerUp(location: Vector2) {
        currentPTexture = (currentPTexture + 1) % powerUpTextures.size
        powerUps += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            textures = powerUpTextures[currentPTexture],
            isRotating = false
        )
    }

    private val Vector2.isOnScreen: Boolean
        get() = x > -10f && x < (viewport.worldWidth + 10f) && y > -10f && y < (viewport.worldHeight + 10f)

    private var totalGameTime = 0f

    private var evenFrame = false

    override fun render(delta: Float) {
        evenFrame = !evenFrame
        totalGameTime += delta

        clearScreen(red = 0f, green = 0f, blue = 0f)

        if (delta == 0f) {
            music?.pause()
        } else {
            if (music?.isPlaying == false) {
                music?.play()
            }
            updateGameState(delta)
        }

        if (totalGameTime == 0f) {
            // game never were unpaused
            return
        }

        viewport.apply(true)
        environmentRenderer.renderBackground(viewport, camera, totalGameTime, flagship.internalPosition)
        batch.use(camera) { batch ->
            enemyShots.forEach { it.draw(batch) }
        }
        if (bombs.isNotEmpty()) {
            shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) { renderer ->
                renderer.color = Color.BLACK
                bombs.forEach {
                    renderer.circle(it.internalPosition.x, it.internalPosition.y, it.internalTimer * 200f)
                }
                renderer.color = Color.WHITE
                bombs.forEach {
                    renderer.circle(it.internalPosition.x, it.internalPosition.y, it.internalTimer * 200f)
                }
            }
        }
        batch.use(camera) { batch ->
            /*enemyShots.forEach { it.draw(batch) }*/
            enemies.forEach { it.draw(batch) }
            shots.forEach { it.draw(batch) }
            powerUps.forEach { it.draw(batch) }
            //bombs.forEach { it.draw(batch) }
            if (!flagship.isInvincible || evenFrame) {
                flagship.draw(batch)
            }
            delayedTextDrawer.updateAndDraw(delta, batch)
        }
        environmentRenderer.renderForeground(viewport, camera, totalGameTime, flagship.internalPosition)
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
            it.color = Color.WHITE
            // using 0.5f because otherwise the left bottom corner is not covered somewhy
            it.rect(0.5f, 0.5f, (viewport.worldWidth - 1f).intFloat, (viewport.worldHeight - 1f).intFloat)
        }
    }

    private fun updateGameState(delta: Float) {
        level.update(delta)
        flagship.update(delta)
        //enemySpawner.update(delta)
        enemies.removeAll { enemy ->
            enemy.update(delta)
            if (enemy.shouldBeRemoved || !enemy.internalPosition.isOnScreen) {
                return@removeAll true
            }
            if (enemy.isAlive) {
                bombs.forEach { bomb ->
                    if (bomb.hits(enemy.internalPosition, bomb.internalTimer * 200f)) {
                        enemy.explode()
                    }
                }
            }
            // TODO: add flagship collision check
            false
        }
        enemyShots.removeAll { enemyShot ->
            enemyShot.update(delta)
            if (!enemyShot.internalPosition.isOnScreen || enemyShot.internalTimer > enemyShot.timeToLive) {
                enemyShot.deadTime -= delta
                if (enemyShot.deadTime < 0f) {
                    return@removeAll true
                }
            }
            bombs.forEach { bomb ->
                if (bomb.hits(enemyShot.internalPosition, bomb.internalTimer * 200f)) {
                    return@removeAll true
                }
            }
            // flagship collision check
            val flagshipHit = !flagship.isInvincible && flagship.collides(enemyShot, 3f, 1.5f)
            if (flagshipHit) {
                if (flagship.startOver()) {
                    bombs += Shot(
                        flagship.internalPosition.cpy(),
                        { _, _, _ -> },
                        explosionTexture,
                        isRotating = false
                    )
                } else {
                    gameOverTimer = 2f
                }
            }
            flagshipHit
        }
        shots.removeAll { shot ->
            shot.update(delta)
            if (!shot.internalPosition.isOnScreen) {
                shot.deadTime -= delta
                if (shot.deadTime < 0f) {
                    return@removeAll true
                }
                return@removeAll true
            }
            // collision check
            var removeShot = false
            for (enemy in enemies) {
                if (enemy.isAlive && shot.hits(enemy.internalPosition, 10f)) {
                    removeShot = true
                    enemy.explode()
                    spawnPowerUp(enemy.internalPosition)
                    defeatToWin -= 1
                    if (defeatToWin == 0) {
                        music?.stop()
                        advance(
                            configuration.copy(
                                livesLeft = flagship.lives,
                                power = flagship.power,
                                shipType = flagship.shipType,
                            )
                        )
                    }
                    break
                }
            }
            removeShot
        }
        powerUps.removeAll { powerUp ->
            powerUp.update(delta)
            if (!powerUp.internalPosition.isOnScreen) {
                return@removeAll true
            }
            // flagship collision check
            val flagshipHit = flagship.collides(powerUp, 8f, 6f)
            if (flagshipHit) {
                flagship.powerUp()
            }
            flagshipHit
        }
        bombs.removeAll { bomb ->
            bomb.update(delta)
            bomb.internalTimer * 200f > viewport.worldHeight * 1.27f
        }

        if (!flagship.isAlive) {
            if (gameOverTimer > 0f) {
                gameOverTimer -= delta
            } else {
                music?.stop()
                showGameOver(configuration)
            }
        }
    }

    fun Flagship.collides(item: Shot, rearDistance: Float, frontDistance: Float): Boolean {
        return flagship.isAlive && (item.hits(rearPosition, rearDistance) || item.hits(frontPosition, frontDistance))
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

    data class Configuration(
        val levelPath: String,
        val sequenceEndAction: EndAction = EndAction.RETURN_TO_MENU,
        var livesLeft: Int = 3,
        var power: Float = 0f,
        var shipType: Int = 0,
    )

    enum class EndAction {
        RETURN_TO_MENU
    }
}
