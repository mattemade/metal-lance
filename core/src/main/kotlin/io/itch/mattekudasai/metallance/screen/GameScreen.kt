package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
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
import io.itch.mattekudasai.metallance.`object`.Bomb
import io.itch.mattekudasai.metallance.`object`.Shot
import io.itch.mattekudasai.metallance.player.Flagship
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
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.pixel.intFloat
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.max
import kotlin.math.min

class GameScreen(
    private val configuration: Configuration,
    private val setRenderMode: (mode: Int, stage: Int) -> Unit,
    private val setTint: (Color) -> Unit,
    private val returnToMainMenu: () -> Unit,
    private val showGameOver: (configuration: Configuration) -> Unit,
    private val advance: (configuration: Configuration) -> Unit,
    private val restart: () -> Unit,
) : KtxScreen,
    KtxInputAdapter, Disposing by Self() {

    private val hudHeight = 8f
    private val tempReusableColor: Color = Color.WHITE.cpy()

    private val flagship: Flagship by remember {
        Flagship(
            viewport.worldWidth,
            viewport.worldHeight,
            hudHeight,
            easyMode = configuration.easyMode,
            explosionTexture,
            configuration.livesLeft,
            configuration.power,
            configuration.charge,
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
    private val energyPods = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val shipUpgrades = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val bombs = mutableDisposableListOf<Bomb>(onDisposed = ::forget).autoDisposing()


    // TODO: pack all the textures in one 2048x2048 atlas to avoid constant rebinding
    private val shotTexture: Texture by remember { Texture("texture/bullet/shot.png".overridable) }
    private val enemyTextures = ('a'..'f').map {
        Texture("texture/enemy/$it.png".overridable).autoDisposing()
    }
    private val explosionTexture: Texture by remember { Texture("texture/explosion.png".overridable) }
    private val enemyShotTextures = listOf("texture/bullet/wave.png").map {
        Texture(it.overridable).autoDisposing()
    }

    //private val powerUpTexture: Texture by remember { Texture("texture/upgrade/power.png".overridable) }
    private val powerUpTextures = (0..2).map { index ->
        (0..9).map { frame ->
            Texture("texture/upgrade/power/p${index}_$frame.png".overridable).autoDisposing() to if (frame == 9) 0.292f else 0.042f
        }
    }
    private var currentPowerTexture = 0

    //private val energyPodTexture: Texture by remember { Texture("texture/upgrade/energy.png".overridable) }
    private val energyPodTextures = (5..5).map { index ->
        ((if (index == 4) 1 else 0)..(if (index == 1 || index == 4) 15 else 9)).map { frame ->
            Texture("texture/upgrade/energy/e${index}_$frame.png".overridable).autoDisposing() to if (frame == 9) 0.292f else 0.042f
        }
    }
    private var currentEnergyTexture = 0

    //private val shipUpgradeTexture: Texture by remember { Texture("texture/upgrade/ship.png") }
    private val shipUpgradeTextures = (0..1).map { index ->
        if (index == 1) {
            listOf(Texture("texture/upgrade/ship.png".overridable).autoDisposing() to 10f)
        } else
            (0..20).map { frame ->
                Texture("texture/upgrade/ship/s${index}_$frame.png".overridable).autoDisposing() to if (frame == 20) 0.292f else 0.042f
            }
    }
    private var currentShipUpgradeTexture = 0
    private var environmentRenderer: EnvironmentRenderer = NoopEnvironmentRenderer
    private var defeatToWin: Int = Integer.MAX_VALUE
    private var gameOverTimer: Float = 0f
    private var isGameStarted = false
    private var isGameEnding = false
    private var fadingFactor = 1f
    private var breathingTime = 1f
    private var maxMusicVolume = 1f

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
            val enemyTexture = enemyTextures[enemyConfiguration.enemyType]
            val initialPosition = Vector2(
                when {
                    Align.isRight(enemyConfiguration.spawnSide) -> viewport.worldWidth + enemyTexture.width / 2f
                    Align.isLeft(enemyConfiguration.spawnSide) -> -enemyTexture.width / 2f
                    else -> enemyConfiguration.spawnSideFactor * viewport.worldWidth
                },
                when {
                    Align.isTop(enemyConfiguration.spawnSide) -> viewport.worldHeight + enemyTexture.height / 2f
                    Align.isBottom(enemyConfiguration.spawnSide) -> hudHeight-enemyTexture.height / 2f
                    else -> enemyConfiguration.spawnSideFactor * viewport.worldHeight
                }
            )

            enemies += Enemy(
                texture = enemyTexture,
                explosionTexture = explosionTexture,
                initialPosition = initialPosition,
                updatePositionDt = enemyConfiguration.updatePositionDt,
                shot = {
                    it.shootingPattern?.onShoot?.invoke(
                        it,
                        flagship.internalPosition,
                        enemyShotTextures[it.shootingPattern?.shotTextureIndex ?: 0]
                    )?.let { enemyShots += it }
                },
                initialHitPoints = enemyConfiguration.initialHitPoints,
                invincibilityPeriod = enemyConfiguration.invincibilityPeriod,
                onRemoved = enemyConfiguration.onRemoved,
                onDefeat = enemyConfiguration.onDefeat,
            ).also { enemy ->
                enemyConfiguration.shootingPatternSubscription.subscribe {
                    enemy.shootingPattern = it.toPattern(
                        viewport.worldWidth,
                        enemyConfiguration.shootingPatternSubscription.tempoProvider
                    )
                }
            }
        },
        winningCondition = { condition, counter ->
            when (condition) {
                "kill" -> defeatToWin = counter
            }
        },
        endSequence = {
            isGameEnding = true
        },
        setRenderMode = setRenderMode,
        setTint = setTint,
        playMusic = { path, volume ->
            music?.stop()
            music = null
            val file = path.overridable
            if (file.exists()) {
                maxMusicVolume = volume
                music = Gdx.audio.newMusic(file).autoDisposing().also {
                    it.play()
                    it.volume = volume
                    it.isLooping = true
                }
            }
        },
        getWorldWidth = { viewport.worldWidth },
        getWorldHeight = { viewport.worldHeight },
    )

    val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "texture/font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = "") + ".,'0123456789:Ж",
            fontLetterWidth = 5,
            fontLetterHeight = 9,
            fontHorizontalSpacing = 1,
            fontVerticalSpacing = 0,
            fontHorizontalPadding = 1,
        )
    }
    val delayedTextDrawer = DelayedTextDrawer(textDrawer, 0.04f)
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
        currentPowerTexture = (currentPowerTexture + 1) % powerUpTextures.size
        powerUps += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            textures = powerUpTextures[currentPowerTexture],
            isRotating = false
        )
    }

    private fun spawnEnergyPod(location: Vector2) {
        currentEnergyTexture = (currentEnergyTexture + 1) % energyPodTextures.size
        energyPods += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            textures = energyPodTextures[currentEnergyTexture],
            isRotating = false
        )
    }

    private fun spawnShipUpgrade(location: Vector2) {
        currentShipUpgradeTexture = (currentShipUpgradeTexture + 1) % shipUpgradeTextures.size
        shipUpgrades += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            textures = shipUpgradeTextures[currentShipUpgradeTexture],
            isRotating = false
        )
    }

    private val SimpleSprite.isOnScreen: Boolean
        get() {
            val halfWidth = width / 2f
            val halfHeight = height / 2f
            return x > -halfWidth && x < (viewport.worldWidth + halfWidth) && y > -halfHeight && y < (viewport.worldHeight + halfHeight)
        }

    /*    private val Vector2.isOnScreen: Boolean
            get() = x > -10f && x < (viewport.worldWidth + 10f) && y > -10f && y < (viewport.worldHeight + 10f)*/

    private var totalGameTime = 0f

    private var evenFrame = false

    override fun render(delta: Float) {
        evenFrame = !evenFrame
        totalGameTime += delta

        clearScreen(red = 0f, green = 0f, blue = 0f)

        if (delta == 0f) {
            music?.pause()
        } else {
            if (!isGameStarted) {
                fadingFactor -= delta
                if (fadingFactor < 0f) {
                    fadingFactor = 0f
                    isGameStarted = true
                }
            }
            if (flagship.isAlive) {
                level.update(delta) // should be updated regardless of the game state
            }
            if (isGameStarted) {
                music?.let {
                    if (!it.isPlaying) {
                        it.play()
                    }
                }
                updateGameState(delta)
            }
            if (isGameEnding) {
                fadingFactor += delta
                if (fadingFactor > 1f) {
                    breathingTime = max(0f, breathingTime - delta)
                    fadingFactor = 1f
                }
                music?.volume = maxMusicVolume * ((1f - fadingFactor) + breathingTime / 2f) / 1.5f
                if (fadingFactor == 1f && breathingTime <= 0f) {
                    music?.stop()
                    music = null
                    if (defeatToWin == 0) {
                        advance(
                            configuration.copy(
                                livesLeft = flagship.lives,
                                power = flagship.power,
                                charge = flagship.charge,
                                shipType = flagship.shipType,
                                usedContinue = false,
                                passedPreviousLevel = true,
                            )
                        )
                    } else if (!flagship.isAlive) {
                        showGameOver(configuration)
                    } else if (isRestarting) {
                        restart()
                    } else {
                        when (configuration.sequenceEndAction) {
                            EndAction.RETURN_TO_MENU -> {
                                returnToMainMenu()
                            }

                            EndAction.NEXT_LEVEL -> advance(
                                configuration.copy(
                                    livesLeft = flagship.lives,
                                    power = flagship.power,
                                    charge = flagship.charge,
                                    shipType = flagship.shipType,
                                    usedContinue = false,
                                    passedPreviousLevel = true,
                                )
                            )
                        }

                    }
                }
            }
        }

        if (totalGameTime == 0f) {
            // game never were unpaused
            return
        }

        viewport.apply(true)
        environmentRenderer.renderBackground(viewport, camera, totalGameTime, flagship.internalPosition)
        batch.use(camera) { batch ->
            powerUps.forEach { it.draw(batch) }
            energyPods.forEach { it.draw(batch) }
            shipUpgrades.forEach { it.draw(batch) }
            enemyShots.forEach { it.draw(batch) }
            enemies.forEach {
                if (it.isAlive && (!it.isInvincible || !evenFrame)) {
                    it.draw(batch)
                }
            }
        }
        bombs.forEach { it.draw(camera) }
        batch.use(camera) { batch ->
            enemies.forEach {
                if (!it.isAlive) {
                    it.draw(batch)
                }
            }
            shots.forEach { it.draw(batch) }
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
            it.rect(0.5f, hudHeight + 0.5f, (viewport.worldWidth - 1f).intFloat, (viewport.worldHeight - 1f - hudHeight).intFloat)
        }
        // hud
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.color = Color.BLACK
            it.rect(0.5f, 0.5f, viewport.worldWidth + 2f, hudHeight)
        }
        batch.use(camera) {
            // lives
            textDrawer.drawText(it, listOf("Ж${if (configuration.easyMode) 99 else min(max(0, flagship.lives), 99)}"), 58f, -11f)
            // power
            it.color = tempReusableColor.set(0.2f, 0.2f, 0.2f, 1f)
            textDrawer.drawText(it, listOf("POWER"), 85f, -11f)
            it.color = Color.WHITE
            textDrawer.drawText(it, listOf("POWER"), 85f, -11f, characterLimit = (flagship.power * 5f).toInt())

            // ship type
            textDrawer.drawText(it, listOf(
                when (flagship.shipType) {
                    0 -> "NORMAL"
                    1 -> "DOUBLE"
                    2 -> "TRIPLE"
                    3 -> "QUADRU"
                    4 -> "QUINTU"
                    else -> "METAL"
                }), 124f, -11f)

            // lance charge
            it.color = tempReusableColor.set(0.2f, 0.2f, 0.2f, 1f)
            textDrawer.drawText(it, listOf("LANCE"), 169f, -11f)
            it.color = Color.WHITE
            textDrawer.drawText(it, listOf("LANCE"), 169f, -11f, characterLimit = (flagship.charge * 5f).toInt())


        }

        if (fadingFactor > 0f) {
            withTransparency {
                shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                    it.color = tempReusableColor.set(0f, 0f, 0f, fadingFactor)
                    it.rect(-1f, -1f, viewport.worldWidth + 2f, viewport.worldHeight + 2f)
                }
            }
        }
    }

    private fun updateGameState(delta: Float) {
        flagship.update(delta)
        //enemySpawner.update(delta)
        enemies.removeAll { enemy ->
            enemy.update(delta)
            if (enemy.shouldBeRemoved) {
                enemy.onRemoved()
                return@removeAll true
            }
            if (enemy.isAlive && !enemy.isInvincible) {
                // flagship collision check
                val flagshipHit = !flagship.isInvincible && flagship.collides(enemy, 3f, 1.5f)
                if (flagshipHit) {
                    enemy.explodeAndSpawnReward()
                    damageFlagship()
                }
            }
            if (enemy.isAlive && !enemy.isInvincible) { // still alive
                bombs.forEach { bomb ->
                    if (bomb.hits(enemy.internalPosition, enemy.width / 2f, enemy)) {
                        enemy.explodeAndSpawnReward()
                    }
                }
            }
            if (enemy.isAlive) { // still alive!!
                if (!enemy.isOnScreen) {
                    enemy.offscreenTimeToDisappear -= delta
                } else if (enemy.offscreenTimeToDisappear != Enemy.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR) {
                    enemy.offscreenTimeToDisappear = Enemy.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR
                }
            }
            (enemy.offscreenTimeToDisappear <= 0f).also { if (it) enemy.onRemoved() }
        }
        enemyShots.removeAll { enemyShot ->
            enemyShot.update(delta)
            if (enemyShot.shouldJustDisappear(delta)) {
                return@removeAll true
            }
            bombs.forEach { bomb ->
                if (bomb.hits(enemyShot.internalPosition, 0f)) {
                    return@removeAll true
                }
            }
            // flagship collision check
            val flagshipHit = !flagship.isInvincible && flagship.collides(enemyShot, 3f, 1.5f)
            if (flagshipHit) {
                damageFlagship()
            }
            flagshipHit
        }
        shots.removeAll { shot ->
            shot.update(delta)
            if (shot.shouldJustDisappear(delta)) {
                return@removeAll true
            }
            // collision check
            var removeShot = false
            for (enemy in enemies) {
                if (enemy.isAlive && !enemy.isInvincible && shot.hits(enemy.internalPosition, 10f)) {
                    removeShot = true
                    enemy.explodeAndSpawnReward()
                    defeatToWin -= 1
                    if (defeatToWin == 0) {
                        isGameEnding = true
                    }
                    break
                }
            }
            removeShot
        }
        powerUps.updatePickups(delta) { flagship.powerUp() }
        energyPods.updatePickups(delta) { flagship.chargeUp() }
        shipUpgrades.updatePickups(delta) { flagship.transform() }
        bombs.removeAll { !it.update(delta) }

        if (!flagship.isAlive) {
            music?.let {
                it.stop()
                music = null
            }
            if (gameOverTimer > 0f) {
                gameOverTimer -= delta
            } else {
                breathingTime = 0f
                isGameEnding = true
            }
        }
    }

    private fun damageFlagship() {
        if (flagship.startOver()) {
            bombs += Bomb(
                flagship.internalPosition.cpy(),
                speed = 400f,
                maxRadius = viewport.worldHeight * 1.27f,
                isEnemy = false,
                stayFor = 0f,
                fadeOutIn = 1f,
                outerColor = Color.WHITE,
                innerColor = Color.BLACK.cpy().apply { a = 0.6f }
            )
        } else {
            gameOverTimer = 2f
        }
    }

    fun MutableList<Shot>.updatePickups(delta: Float, onPickedUp: () -> Unit) {
        removeAll { pickup ->
            pickup.update(delta)
            if (pickup.shouldJustDisappear(delta)) {
                return@removeAll true
            }
            // flagship collision check
            val flagshipHit = flagship.collides(pickup, 8f, 6f)
            if (flagshipHit) {
                onPickedUp()
            }
            flagshipHit
        }
    }

    private fun Enemy.explodeAndSpawnReward() {
        hit()?.let {
            when (it) {
                'p' -> spawnPowerUp(internalPosition)
                'e' -> spawnEnergyPod(internalPosition)
                's' -> spawnShipUpgrade(internalPosition)
                'w' -> if (flagship.isAlive) {
                    advance(configuration)
                }
            }

        }
    }

    private fun Shot.shouldJustDisappear(delta: Float): Boolean {
        if (!isOnScreen) {
            offscreenTimeToDisappear -= delta
            if (offscreenTimeToDisappear < 0f) {
                return true
            }
        } else if (internalTimer > timeToLive) {
            return true
        } else if (offscreenTimeToDisappear != Shot.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR) {
            offscreenTimeToDisappear = Shot.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR
        }
        return false
    }

    private fun Flagship.collides(enemy: Enemy, rearDistance: Float, frontDistance: Float): Boolean {
        val threshold = enemy.width / 2f
        return flagship.isAlive && (
            enemy.internalPosition.dst(rearPosition) < rearDistance + threshold ||
                enemy.internalPosition.dst(frontPosition) < frontDistance + threshold
            )
    }

    private fun Flagship.collides(item: Shot, rearDistance: Float, frontDistance: Float): Boolean {
        return flagship.isAlive && (item.hits(rearPosition, rearDistance) || item.hits(frontPosition, frontDistance))
    }

    override fun resize(width: Int, height: Int) {
        viewport.setWorldSize(width.toFloat(), height.toFloat())
        viewport.setScreenSize(width, height)
    }

    private var isRestarting = false
    override fun keyDown(keycode: Int): Boolean {
        if (Gdx.app.logLevel == Application.LOG_DEBUG) {
            if (keycode == Keys.R) {
                isRestarting = true
                isGameEnding = true
            } else if (keycode == Keys.N) {
                music?.stop()
                music = null
                returnToMainMenu()
            } else if (keycode == Keys.M) {
                music?.stop()
                music = null
                advance(configuration)
            }
        }
        return flagship.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return flagship.keyUp(keycode)
    }

    data class Configuration(
        val levelPath: String,
        val sequenceEndAction: EndAction = EndAction.NEXT_LEVEL,
        var livesLeft: Int = 3,
        var power: Float = 0f,
        var charge: Float = 0f,
        var shipType: Int = 0,
        var usedContinue: Boolean = false,
        var passedPreviousLevel: Boolean = false,
        var easyMode: Boolean = false,
    )

    enum class EndAction {
        RETURN_TO_MENU,
        NEXT_LEVEL,
    }
}
