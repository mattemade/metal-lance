package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
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
import io.itch.mattekudasai.metallance.enemy.ShootingPattern.Companion.toPatternInt
import io.itch.mattekudasai.metallance.`object`.Bomb
import io.itch.mattekudasai.metallance.`object`.Shot
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.screen.game.CityEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.CloudsEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.EnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.NoopEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.SimulationEnvironmentRenderer
import io.itch.mattekudasai.metallance.screen.game.SpaceEnvironmentRenderer
import io.itch.mattekudasai.metallance.stage.Level
import io.itch.mattekudasai.metallance.util.collision.collides
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.disposing.mutableDisposableListOf
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite
import io.itch.mattekudasai.metallance.util.drawing.withTransparency
import io.itch.mattekudasai.metallance.util.files.overridable
import io.itch.mattekudasai.metallance.util.pixel.intFloat
import io.itch.mattekudasai.metallance.util.sound.playLow
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
    private val tempReusableColor2: Color = Color.WHITE.cpy()

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
            ::shoot,
            tempoProvider = { level.musicTempo },
            spawnLanceBomb = {
                bombs += Bomb(
                    flagship.internalPosition.cpy(),
                    400f,
                    60f,
                    false,
                    0f,
                    1f,
                    Color.WHITE,
                    Color.BLACK.cpy().apply { a = 0.6f },
                    shapeRenderer
                )
            }
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
    private val shieldUpgrades = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val bombs = mutableDisposableListOf<Bomb>(onDisposed = ::forget).autoDisposing()
    private val tempBombs = mutableListOf<Bomb>()


    // TODO: pack all the textures in one 2048x2048 atlas to avoid constant rebinding
    private val shotTexture: Texture by remember { Texture("texture/bullet/shot.png".overridable) }
    private val enemyTextures = ('a'..'i').map {
        Texture("texture/enemy/$it.png".overridable).autoDisposing()
    }
    private val topShellTexture: Texture by remember { Texture("texture/enemy/top_shell.png".overridable) }
    private val bottomShellTexture: Texture by remember { Texture("texture/enemy/bottom_shell.png".overridable) }
    private val explosionTexture: Texture by remember { Texture("texture/explosion.png".overridable) }
    private val enemyShotTextures = listOf(
        "texture/bullet/wave.png",
        "texture/bullet/invisible.png",
        "texture/bullet/laser.png",
        "texture/bullet/steam.png",
    ).map {
        Texture(it.overridable).autoDisposing()
    }
    private val powerUpTexture: Texture by remember { Texture("texture/upgrade/power.png".overridable) }
    private val energyPodTexture: Texture by remember { Texture("texture/upgrade/energy.png".overridable) }
    private val shipUpgradeTexture: Texture by remember { Texture("texture/upgrade/ship.png".overridable) }
    private val shieldUpgradeTexture: Texture by remember { Texture("texture/upgrade/shield.png".overridable) }

    private var environmentRenderer: EnvironmentRenderer = NoopEnvironmentRenderer
    private var defeatToWin: Int = Integer.MAX_VALUE
    private var gameOverTimer: Float = 0f
    private var isGameStarted = false
    private var isGameEnding = false
    private var fadingFactor = 1f
    private var breathingTime = 1f
    private var maxMusicVolume = 1f
    private var musicFadesInTotal = 0f
    private var musicFadesIn = 0f

    private val enemyHitSound: Sound by remember { Gdx.audio.newSound("sound/enemy_hit.ogg".overridable) }
    private val enemyExplodeSound: Sound by remember { Gdx.audio.newSound("sound/explosion.ogg".overridable) }
    private val enemyShotSound: Sound by remember { Gdx.audio.newSound("sound/enemy_shot.ogg".overridable) }
    private val shieldSound: Sound by remember { Gdx.audio.newSound("sound/shield.ogg".overridable) }
    private var shieldSoundPlaying = -1L
    private var shouldPlayEnemyShot: Boolean = false
    private var boss: Enemy? = null
    private val levelTintColor = Color.WHITE.cpy()
    private val screamingTintColor = Color.WHITE.cpy()

    private val isLastStage = configuration.levelPath.contains("stage3")
    private var expectedBossCount = if (isLastStage) 1 else 1
    private var isFinalBoss = isLastStage
    private var endImmediatelyAfterLastBoss = isLastStage

    private var finalBossOpening = 0f
    private var openingWaits = 0f
    private var openingGoesUp = false
    private var openingGoesDown = false

    private val level = Level(
        scriptFile = configuration.levelPath.overridable,
        setBackground = {
            environmentRenderer = when (it) {
                "simulation" -> environmentRenderer as? SimulationEnvironmentRenderer
                    ?: SimulationEnvironmentRenderer().autoDisposing()

                "city" -> environmentRenderer as? CityEnvironmentRenderer ?: CityEnvironmentRenderer().autoDisposing()
                "space" -> environmentRenderer as? SpaceEnvironmentRenderer
                    ?: SpaceEnvironmentRenderer().autoDisposing()

                "clouds" -> environmentRenderer as? CloudsEnvironmentRenderer
                    ?: CloudsEnvironmentRenderer().autoDisposing()

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
                    Align.isBottom(enemyConfiguration.spawnSide) -> hudHeight - enemyTexture.height / 2f
                    else -> enemyConfiguration.spawnSideFactor * viewport.worldHeight
                }
            )

            val addingBoss = Enemy(
                texture = enemyTexture,
                explosionTexture = explosionTexture,
                initialPosition = initialPosition,
                updatePositionDt = enemyConfiguration.updatePositionDt,
                shot = { spawnEnemyShot(it, audible = !(isFinalBoss && enemyConfiguration.isBoss)) },
                initialHitPoints = enemyConfiguration.initialHitPoints,
                invincibilityPeriod = enemyConfiguration.invincibilityPeriod,
                onRemoved = { enemyConfiguration.onRemoved(it) },
                onDefeat = { enemyConfiguration.onDefeat() },
                onStageDefeat = enemyConfiguration.onStageDefeat,
                hitSound = enemyHitSound,
                explodeSound = enemyExplodeSound,
                isBoss = enemyConfiguration.isBoss,
                isBaloon = enemyConfiguration.enemyType == 6, // 'G'
                screaming = { remainingFactor ->
                    screamingTintColor.set(levelTintColor)
                    if (remainingFactor > 0.5f) {
                        stageSwipe = true
                    } else if (remainingFactor > 0f) {
                        stageSwipe = false
                        screamingTintColor.r = levelTintColor.r + (1f - levelTintColor.r) * remainingFactor
                        screamingTintColor.g = levelTintColor.g * (1f - remainingFactor)
                        screamingTintColor.b = levelTintColor.b * (1f - remainingFactor)
                    } else {
                        screamingTintColor.set(levelTintColor)
                    }
                    setTint(screamingTintColor)
                }
            ).also { enemy ->
                enemyConfiguration.shootingPatternSubscription.subscribe(enemy) {
                    enemy.shootingPattern = it.toPattern(
                        viewport.worldWidth,
                        enemyConfiguration.shootingPatternSubscription.tempoProvider
                    )
                }
            }
            if (enemyConfiguration.isBoss) {
                boss = addingBoss
                if (isFinalBoss && expectedBossCount == 1) {
                    addingBoss.timeToMortal = 0.5f
                    bombs += Bomb(
                        addingBoss.internalPosition,
                        400f,
                        60f,
                        true,
                        Float.MAX_VALUE,
                        0F,
                        Color.BLACK.cpy(),
                        Color.WHITE.cpy().apply { a = 0.5f },
                        shapeRenderer,
                    )
                }
            }
            enemies += addingBoss
            if (enemyConfiguration.isBoss && isFinalBoss && expectedBossCount < 3) {
                enemies += Enemy(
                    topShellTexture,
                    explosionTexture,
                    Vector2(),
                    shot = {},
                    hitSound = enemyHitSound,
                    explodeSound = enemyExplodeSound,
                    isBaloon = false,
                    isBoss = false,
                    isTopShell = true,
                    onDefeat = { null },
                    onRemoved = { null },
                    onStageDefeat = { null },
                    screaming = {},
                    anchor = Triple(addingBoss, Vector2(0f, 49f), { finalBossOpening }),
                    initialHitPoints = Int.MAX_VALUE
                )
                enemies += Enemy(
                    bottomShellTexture,
                    explosionTexture,
                    Vector2(),
                    shot = {},
                    hitSound = enemyHitSound,
                    explodeSound = enemyExplodeSound,
                    isBaloon = false,
                    isBoss = false,
                    isBottomShell = true,
                    onDefeat = { null },
                    onRemoved = { null },
                    onStageDefeat = { null },
                    screaming = {},
                    anchor = Triple(addingBoss, Vector2(0f, -49f), { -finalBossOpening }),
                    initialHitPoints = Int.MAX_VALUE
                )
                spawnFinalBossCannons(addingBoss)
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
        setTint = {
            levelTintColor.set(it)
            setTint(levelTintColor)
        },
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
        seekMusic = {
            music?.position = it
        },
        fadeMusicOut = { forTime ->
            musicFadesInTotal = forTime
            musicFadesIn = forTime
        },
        getWorldWidth = { viewport.worldWidth },
        getWorldHeight = { viewport.worldHeight },
    )

    private fun spawnEnemyShot(it: Enemy, audible: Boolean = true) {
        if (audible) {
            shouldPlayEnemyShot = enemyShotSoundCooldown < 0f
        }
        val shotTextureIndex = it.shootingPattern?.shotTextureIndex
        val isCoveredWithBomb = shotTextureIndex?.let { it > 127 } ?: false
        it.shootingPattern?.onShoot?.invoke(
            it,
            flagship.internalPosition,
            enemyShotTextures[if (isCoveredWithBomb) 1 else shotTextureIndex?.takeIf { it < enemyShotTextures.size }
                ?: 0]
        )?.let {
            enemyShots += it
            if (isCoveredWithBomb) {
                bombs += it.map {
                    Bomb(
                        it.internalPosition,
                        400f,
                        10f,
                        isEnemy = true,
                        it.timeToLive,
                        0.25f,
                        Color.BLACK,
                        Color.WHITE.cpy().apply { a = 0.4f },
                        shapeRenderer,
                    )
                }
            }
        }
    }

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
    val delayedTextDrawer = DelayedTextDrawer(textDrawer, { 0.04f })
    private var music: Music? = null

    private fun spawnShot(offsetX: Float = 0f, offsetY: Float = 0f, angleDeg: Float = 0f): Shot =
        Shot(
            initialPosition = flagship.internalPosition.cpy().add(offsetX, offsetY),
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

    private fun spawnFinalBossCannon(
        finalBoss: Enemy,
        textureId: Int,
        health: Int,
        offset: Vector2,
        reward: Char,
        shootingPattern: String
    ) {
        enemies += Enemy(
            enemyTextures[textureId],
            explosionTexture,
            Vector2(),
            shot = { spawnEnemyShot(it) },
            hitSound = enemyHitSound,
            explodeSound = enemyExplodeSound,
            isBaloon = false,
            isBoss = false,
            onDefeat = { reward },
            onRemoved = { null },
            onStageDefeat = { null },
            screaming = {},
            anchor = Triple(finalBoss, offset) { if (offset.y > 0f) finalBossOpening else -finalBossOpening },
            initialHitPoints = health,
            keepOffscreen = true,
            invincibilityPeriod = 0.5f
        ).also { enemy ->
            enemy.shootingPattern = shootingPattern.toPatternInt().toPattern(
                viewport.worldWidth,
                { level.musicTempo }
            )
        }
    }

    private fun spawnFinalBossCannons(finalBoss: Enemy) {
        spawnFinalBossCannon(finalBoss, 5, 5, Vector2(-97f, 10f), 'p', "LHPPPPPGGGGGG")
        spawnFinalBossCannon(finalBoss, 3, 2, Vector2(-78f, 39f), 'e', "B4XRFGG")
        spawnFinalBossCannon(finalBoss, 1, 1, Vector2(-60f, 63f), 'p', "BF")
        spawnFinalBossCannon(finalBoss, 1, 1, Vector2(-60f, -63f), 'e', "BF")
        spawnFinalBossCannon(finalBoss, 3, 3, Vector2(-78f, -39f), 'p', "B4XRFGG")
        spawnFinalBossCannon(finalBoss, 5, 5, Vector2(-97f, -10f), 'e', "LHPPPPPGGGGGG")
    }

    private fun spawnPowerUp(location: Vector2) {
        powerUps += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            texture = powerUpTexture,
            isRotating = false
        )
    }

    private fun spawnEnergyPod(location: Vector2) {
        energyPods += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            texture = energyPodTexture,
            isRotating = false
        )
    }

    private fun spawnShipUpgrade(location: Vector2) {
        shipUpgrades += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            texture = shipUpgradeTexture,
            isRotating = false
        )
    }

    private fun spawnShieldUpgrade(location: Vector2) {
        shieldUpgrades += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            texture = shieldUpgradeTexture,
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
    private var totalFrameCount = 0
    private var previousSecondFps = 0
    private var totalFrameCountLastSecond = 0
    private var logFpsIn = 1f

    private var evenFrame = false
    private var enemyShotSoundCooldown = 0f
    private var lastEnemyShotAt = 0f
    private var stage = 1
    private var stageSwipe = false

    override fun render(delta: Float) {
        evenFrame = !evenFrame
        totalGameTime += delta
        if (Gdx.app.logLevel == Application.LOG_DEBUG && delta > 0f) {
            totalFrameCount++
            totalFrameCountLastSecond++
            logFpsIn -= delta
            if (logFpsIn < 0f) {
                logFpsIn += 1f
                Gdx.app.debug("fps", "$totalFrameCountLastSecond, avg: ${totalFrameCount / totalGameTime}")
                previousSecondFps = totalFrameCountLastSecond
                totalFrameCountLastSecond = 0
            }
        }
        enemyShotSoundCooldown -= delta

        clearScreen(red = 0f, green = 0f, blue = 0f)

        if (delta == 0f) {
            music?.pause()
        } else {
            if (stageSwipe) {
                stage++
                if (stage == 4) stage = 1
                setRenderMode(4, stage)
            } else if (stage > 0) {
                stage = 0
                setRenderMode(4, stage)
            }
            if (!isGameStarted) {
                fadingFactor -= delta
                if (fadingFactor < 0f) {
                    fadingFactor = 0f
                    isGameStarted = true
                }
            }
            if (flagship.isAlive) {
                level.update(delta) // should be updated regardless of the game state
                if (shouldPlayEnemyShot) {
                    val fromLastShot = totalGameTime - lastEnemyShotAt
                    lastEnemyShotAt = totalGameTime
                    shouldPlayEnemyShot = false
                    if (enemyShotSoundCooldown < 0f) {
                        enemyShotSound.playLow(0.05f + 0.05f * min(1f, fromLastShot * 2f))
                    }
                    enemyShotSoundCooldown = 0.05f
                }
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

        music?.let { music ->
            if (musicFadesIn > 0f) {
                musicFadesIn = max(0f, musicFadesIn - delta)
                music.volume = musicFadesIn * maxMusicVolume / musicFadesInTotal
            }
        }

        viewport.apply(true)
        environmentRenderer.renderBackground(
            viewport,
            camera,
            totalGameTime,
            flagship.internalPosition.takeIf { flagship.isAlive })
        bombs.forEach { if (it.isEnemy) it.draw(camera) }

        //withTransparency {
        batch.use(camera) { batch ->
            enemies.forEach {
                if (!it.isAlive) {
                    it.draw(batch)
                }
            }
            powerUps.forEach { it.draw(batch) }
            energyPods.forEach { it.draw(batch) }
            shipUpgrades.forEach { it.draw(batch) }
            shieldUpgrades.forEach { it.draw(batch) }
            shots.forEach { it.draw(batch) }
            if (!flagship.isInvincible || evenFrame) {
                flagship.draw(batch)
            }
            enemyShots.forEach {
                it.draw(batch)
            }
            enemies.forEach {
                if (it.isAlive && (!it.isInvincible || !evenFrame)) {
                    it.draw(batch)
                }
            }
        }
        //}
        bombs.forEach { if (!it.isEnemy) it.draw(camera) }
        if (flagship.visibleTrailFactor > 0f) {
            withTransparency {
                shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
                    val fullWidth = flagship.endLancingPosition.x - flagship.startLancingPosition.x
                    val visibleWidth = fullWidth * flagship.visibleTrailFactor
                    val startX = flagship.endLancingPosition.x - visibleWidth
                    it.line(
                        startX,
                        flagship.startLancingPosition.y,
                        flagship.endLancingPosition.x,
                        flagship.startLancingPosition.y,
                        tempReusableColor.set(Color.WHITE).apply { a = 0f },
                        tempReusableColor2.set(Color.WHITE).apply { a = flagship.visibleTrailFactor }
                    )
                }
            }
        }
        batch.use(camera) { batch ->

            //bombs.forEach { it.draw(batch) }

            delayedTextDrawer.updateAndDraw(delta, batch)
        }
        environmentRenderer.renderForeground(viewport, camera, totalGameTime, flagship.internalPosition)
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
            it.color = Color.WHITE
            // using 0.5f because otherwise the left bottom corner is not covered somewhy
            it.rect(
                0.5f,
                hudHeight + 0.5f,
                (viewport.worldWidth - 1f).intFloat,
                (viewport.worldHeight - 1f - hudHeight).intFloat
            )
        }
        // hud
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.color = Color.BLACK
            it.rect(0.5f, 0.5f, viewport.worldWidth + 2f, hudHeight)
            boss?.let { boss ->
                it.rect(10f, 230f, 235f, 5f)
                it.color = Color.WHITE
                it.rect(11f, 231f, boss.hitPoints * 234f / boss.initialHitPoints, 3f)

            }
        }

        batch.use(camera) {
            // lives
            textDrawer.drawText(
                it,
                livesLeftText[if (configuration.easyMode || flagship.lives > 3) 4 else max(0, flagship.lives)],
                58f,
                -11f
            )
            // power
            it.color = tempReusableColor.set(0.2f, 0.2f, 0.2f, 1f)
            textDrawer.drawText(it, powerText, 85f, -11f)
            it.color = Color.WHITE
            textDrawer.drawText(it, powerText, 85f, -11f, characterLimit = flagship.power)


            // ship type
            textDrawer.drawText(it, shipTypeText[flagship.shipType], 124f, -11f)

            // lance charge
            it.color = tempReusableColor.set(0.2f, 0.2f, 0.2f, 1f)
            textDrawer.drawText(it, lanceText, 169f, -11f)
            it.color = Color.WHITE
            textDrawer.drawText(it, lanceText, 169f, -11f, characterLimit = flagship.charge)

            if (Gdx.app.logLevel == Application.LOG_DEBUG) {
                textDrawer.drawText(it, listOf("$previousSecondFps"), 10f, 10f)
            }
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
        if (isFinalBoss && expectedBossCount < 3 && boss != null) {
            if (enemies.size == 3) {
                if (finalBossOpening >= 20f) {
                    openingGoesUp = false
                    openingWaits -= delta
                    if (openingWaits <= 0f) {
                        boss?.shootingPattern = null
                        spawnFinalBossCannons(boss!!)
                        openingGoesDown = true
                    }
                } else if (finalBossOpening == 0f) {
                    openingWaits = 2f
                    if (!openingGoesUp) {
                        boss?.shootingPattern =
                            "S9DRM".toPatternInt().toPattern(viewport.worldWidth, { level.musicTempo })
                        openingGoesUp = true
                    }
                }
                if (openingGoesUp) {
                    finalBossOpening += delta * 10f
                }
            }
            if (openingGoesDown) {
                finalBossOpening -= delta * 10f
                if (finalBossOpening <= 0f) {
                    finalBossOpening = 0f
                    openingGoesDown = false
                }
            }
        }
        flagship.update(delta)
        //enemySpawner.update(delta)
        enemies.removeAll { enemy ->
            if (isFinalBoss && expectedBossCount < 3 && enemy == boss && (openingGoesUp || openingGoesDown || openingWaits > 0f)) {
                enemy.updateShootingOnly(delta)
                // do not move this boss while he is standing and opening/closing!
            } else {
                enemy.update(delta)
            }
            if (enemy.shouldBeRemoved) {
                enemy.onRemoved(enemy)
                return@removeAll true
            }
            if (enemy.isAlive && !enemy.isInvincible) {
                // flagship collision check
                val flagshipHit = !flagship.isInvincible && flagship.isAlive && (!enemy.isShell && collides(
                    enemy.previousPosition,
                    enemy.internalPosition,
                    flagship.previousPosition,
                    flagship.internalPosition,
                    1.5f
                ) || flagship.collides(enemy, 3f, 1.5f))
                if (flagshipHit) {
                    enemy.explodeAndSpawnReward(damage = if (flagship.isLancing) 3 else 1)
                    if (!enemy.isBaloon) {
                        damageFlagship(force = enemy.isShell)
                    }
                }
            }
            if (enemy.isAlive && !enemy.isInvincible && !enemy.isShell && !(isLastStage && enemy.isBoss)) { // still alive
                bombs.forEach { bomb ->
                    if (!bomb.isEnemy) {
                        if (bomb.hits(enemy.internalPosition, enemy.width / 2f, enemy)) {
                            enemy.explodeAndSpawnReward(2)
                        }
                    }
                }
            }
            if (enemy.isAlive) { // still alive!!
                if (!enemy.isOnScreen) {
                    enemy.offscreenTimeToDisappear -= delta
                } else if (!enemy.isShell && !enemy.keepOffscreen && enemy.offscreenTimeToDisappear != Enemy.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR) {
                    enemy.offscreenTimeToDisappear = Enemy.DEFAULT_OFFSCREEN_TIME_TO_DISAPPEAR
                }
            }
            (enemy.offscreenTimeToDisappear <= 0f).also { if (it) enemy.onRemoved(enemy) }
        }
        enemyShots.removeAll { enemyShot ->
            enemyShot.update(delta)
            if (enemyShot.shouldJustDisappear(delta)) {
                enemyShot.markedForRemoval = true
                return@removeAll true
            }
            bombs.forEach { bomb ->
                if (!bomb.isEnemy && bomb.hits(enemyShot.internalPosition, 0f)) {
                    enemyShot.markedForRemoval = true
                    return@removeAll true
                }
            }
            // flagship collision check
            val flagshipHit = !flagship.isInvincible && flagship.isAlive && collides(
                enemyShot.previousPosition,
                enemyShot.internalPosition,
                flagship.previousPosition,
                flagship.internalPosition,
                1.5f
            )
            if (flagshipHit) {
                damageFlagship()
            }
            enemyShot.markedForRemoval = flagshipHit
            flagshipHit
        }
        shots.removeAll { shot ->
            shot.update(delta)
            if (shot.shouldJustDisappear(delta)) {
                shot.markedForRemoval = true
                return@removeAll true
            }
            bombs.forEach { bomb ->
                if (bomb.isEnemy && bomb.hits(shot.internalPosition, 0f)) {
                    shot.markedForRemoval = true
                    return@removeAll true
                }
            }
            // collision check
            var removeShot = false
            for (enemy in enemies) {
                if (enemy.isAlive && !enemy.isInvincible && shot.hits(
                        enemy.internalPosition,
                        if (enemy.isShell) enemy.height / 2f else enemy.width / 2f
                    )
                ) {
                    removeShot = true
                    enemy.explodeAndSpawnReward()
                    defeatToWin -= 1
                    if (defeatToWin == 0) {
                        isGameEnding = true
                    }
                    break
                }
            }
            shot.markedForRemoval = removeShot
            removeShot
        }
        powerUps.updatePickups(delta) { flagship.powerUp() }
        energyPods.updatePickups(delta) { flagship.chargeUp() }
        shipUpgrades.updatePickups(delta) { flagship.transform() }
        shieldUpgrades.updatePickups(delta) { createShield() }
        bombs.removeAll {
            val shouldBeRemoved = !it.update(delta)
            if (!shouldBeRemoved && it.isEnemy && !flagship.isInvincible && it.hits(flagship.internalPosition, 1f)) {
                damageFlagship()
            }
            shouldBeRemoved
        }
        bombs += tempBombs
        tempBombs.clear()

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

    private fun createShield() {
        flagship.playPickupSound()
        /* shieldSoundPlaying = shieldSound.play()
         shieldSound.setVolume(shieldSoundPlaying, 0.05f)
         shieldSound.setLooping(shieldSoundPlaying, true)*/
        Gdx.app.debug("sound", "started shield $shieldSoundPlaying")
        var looping = false
        bombs += Bomb(
            flagship.internalPosition,
            200f,
            20f,
            false,
            10f,
            2f,
            Color.WHITE,
            Color.BLACK.cpy().apply { a = 0.6f },
            shapeRenderer,
            progress = {
                /*if (it == 0f) {
                    Gdx.app.debug("sound", "stopping shield $shieldSoundPlaying")
                    //shieldSound.setLooping(shieldSoundPlaying, false)
                    //shieldSound.stop(shieldSoundPlaying)
                    shieldSound.stop(shieldSoundPlaying)
                    shieldSoundPlaying = -1
                } else if (it < 1f) {
                    Gdx.app.debug("sound", "voluming shield $shieldSoundPlaying")
                    shieldSound.setLooping(shieldSoundPlaying, true)
                    shieldSound.setVolume(shieldSoundPlaying, 0.05f * it)
                } else if (!looping) {
                    Gdx.app.debug("sound", "looping $shieldSoundPlaying")
                    looping = true
                    shieldSound.setLooping(shieldSoundPlaying, true)
                }*/
            }
        )
    }

    private fun damageFlagship(force: Boolean = false) {
        if (flagship.hit(force)) {
            tempBombs += Bomb(
                flagship.internalPosition.cpy(),
                speed = 500f,
                maxRadius = viewport.worldHeight * 1.27f,
                isEnemy = false,
                stayFor = 0f,
                fadeOutIn = 0.5f,
                outerColor = Color.WHITE,
                innerColor = Color.BLACK.cpy().apply { a = 0.6f },
                shapeRenderer,
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

    private fun Enemy.explodeAndSpawnReward(damage: Int = 1) {
        if (this == boss && isFinalBoss && finalBossOpening <= 0.5f) {
            return
        }
        hit(damage, keepAlive = isFinalBoss && this == boss)?.let {
            when (it) {
                'p' -> spawnPowerUp(internalPosition)
                'e' -> spawnEnergyPod(internalPosition)
                'S' -> spawnShipUpgrade(internalPosition)
                's' -> spawnShieldUpgrade(internalPosition)
                'w' -> if (flagship.isAlive) {
                    advance(configuration)
                }
            }
        }
        if (isBoss && hitPoints == 0) {
            expectedBossCount--
            if (expectedBossCount == 0) {
                music?.stop()
                music = null
                if (endImmediatelyAfterLastBoss) {
                    isGameEnding = true
                    fadingFactor = 1f
                    breathingTime = 0f
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

    private val tempVector = Vector2()

    private fun Flagship.collides(enemy: Enemy, rearDistance: Float, frontDistance: Float): Boolean {

        val threshold = if (enemy.isShell) enemy.height / 2f else enemy.width / 2f
        /*        if (enemy.isTopShell || enemy.isBottomShell) {
                    val halfHeight = enemy.height / 2f
                    tempVector.set(enemy.x + threshold, enemy.y + if (enemy.isBottomShell) enemy.height else 0f)
                    Gdx.app.debug("hit", "${flagship.internalPosition} with $tempVector")
                    return flagship.isAlive && (tempVector.dst(rearPosition) < rearDistance + halfHeight ||
                        tempVector.dst(frontPosition) < frontDistance + halfHeight
                        )
                }*/
        return (flagship.isAlive && (
            enemy.internalPosition.dst(rearPosition) < rearDistance + threshold ||
                enemy.internalPosition.dst(frontPosition) < frontDistance + threshold
            )).also {
            if (it) {
                Gdx.app.debug(
                    "hit",
                    "flagship at ${flagship.internalPosition} hits with ${enemy.internalPosition} by $threshold"
                )
                Gdx.app.debug("hit", "distance is ${flagship.internalPosition.dst(enemy.initialPosition)}")
                val a = 0
                val b = a + 1
            }
        }
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

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return flagship.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return flagship.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return flagship.touchUp(screenX, screenY, pointer, button)
    }

    data class Configuration(
        val levelPath: String,
        val sequenceEndAction: EndAction = EndAction.NEXT_LEVEL,
        var livesLeft: Int = 3,
        var power: Int = 0,
        var charge: Int = 0,
        var shipType: Int = 0,
        var usedContinue: Boolean = false,
        var passedPreviousLevel: Boolean = false,
        var easyMode: Boolean = false,
    )

    enum class EndAction {
        RETURN_TO_MENU,
        NEXT_LEVEL,
    }

    companion object {
        private val lanceText = listOf("LANCE")
        private val powerText = listOf("POWER")
        private val livesLeftText = listOf(
            "Ж0",
            "Ж1",
            "Ж2",
            "Ж3",
            "Ж99",
        ).map { listOf(it) }
        private val shipTypeText = listOf(
            "NORMAL",
            "DOUBLE",
            "TRIPLE",
            "QUADRU",
            "QUINTU",
        ).map { listOf(it) }
    }
}
