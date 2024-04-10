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
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.player.Shot
import io.itch.mattekudasai.metallance.stage.Level
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.disposing.mutableDisposableListOf
import io.itch.mattekudasai.metallance.util.drawing.DelayedTextDrawer
import io.itch.mattekudasai.metallance.util.drawing.MonoSpaceTextDrawer
import io.itch.mattekudasai.metallance.util.files.overridable
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
import kotlin.math.abs
import kotlin.math.sign

class GameScreen(playMusic: Boolean = true, private val setRenderMode: (mode: Int, stage: Int) -> Unit, private val setTint: (Color) -> Unit) : KtxScreen,
    KtxInputAdapter, Disposing by Self() {

    private val flagship: Flagship by remember {
        Flagship(
            viewport.worldWidth,
            viewport.worldHeight,
            explosionTexture,
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
    private val enemyTexture: Texture by remember { Texture("texture/saucer.png".overridable) }
    private val explosionTexture: Texture by remember { Texture("texture/explosion.png".overridable) }
    private val enemyShotTexture: Texture by remember { Texture("texture/bullet/wave.png".overridable) }
    private val powerUpTexture: Texture by remember { Texture("texture/upgrade/power.png".overridable) }

    private var renderBackground: () -> Unit = {}

    private val level = Level(
        scriptFile = "levels/tutorial.txt".overridable,
        setBackground = {
            renderBackground = when (it) {
                "simulation" -> {
                    {
                        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) { renderer ->
                            for (i in 0..1) {
                                val density = 8 - i * 3
                                val netWidth = viewport.worldWidth / density
                                val netHeight = viewport.worldHeight / density
                                val xOffset =
                                    netWidth - (((totalGameTime * (i + 1).toFloat()) * 20f + flagship.internalPosition.x * (i + 1).toFloat() * 0.025f) % netWidth)
                                val yOffset =
                                    netHeight - (flagship.internalPosition.y * (i + 1).toFloat() * 0.125f) % netHeight
                                for (j in 0..density step 2) {
                                    val x = xOffset + j * netWidth
                                    val y = yOffset + j * netHeight
                                    renderer.color = Color.WHITE.cpy().mul(1f / (128f - i * 100f))
                                    renderer.rect(x, -1f, netWidth, viewport.worldHeight + 2f)
                                    renderer.rect(-1f, y, viewport.worldWidth + 2f, netHeight)
                                }
                            }

                        }
                    }
                }

                else -> {
                    { }
                }
            }
        },
        showText = {
            delayedTextDrawer.startDrawing(
                it.text,
                it.positionX * viewport.worldWidth,
                it.positionY * viewport.worldHeight
            )
        },
        spawnEnemy = {
            val initialPosition = Vector2()
            if (Align.isRight(it.alignment)) {
                initialPosition.set(viewport.worldWidth + 8f, it.alignmentFactor * viewport.worldHeight)
            }
            // TODO: nextShootingDelay and shot should be set from pattern
            enemies += Enemy(
                texture = enemyTexture,
                explosionTexture = explosionTexture,
                initialPosition = initialPosition,
                updatePositionDt = it.updatePositionDt,
                nextShootingDelay = { 2f },
                shot = ::spawnHomingEnemyShot
            )
        },
        endSequence = {},
        setRenderMode = setRenderMode,
        setTint = setTint
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
    private val music: Music by remember { Gdx.audio.newMusic("music/stage1.ogg".overridable) }

    init {
        Gdx.input.inputProcessor = this
        if (playMusic) {
            music.play()
            music.isLooping = true
        }
    }

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

    private fun spawnSunEnemyShot(enemy: Enemy) {
        val step = when {
            enemy.internalTimer < 1f -> 90
            enemy.internalTimer < 2.5f -> 45
            enemy.internalTimer < 4f -> 30
            else -> 15
        }
        val speedFactor = when {
            enemy.internalTimer < 1f -> 0.5f
            enemy.internalTimer < 2.5f -> 0.75f
            enemy.internalTimer < 4f -> 1f
            else -> 1.25f
        }
        for (angle in 0..360 step step) {
            var wasSet = false
            var wasHomed = false
            enemyShots += Shot(
                enemy.internalPosition.cpy(),
                { shot, time, delta ->
                    if (!wasSet) {
                        wasSet = true
                        shot.direction.set(Shot.SPEED_SLOW * speedFactor, 0f).rotateDeg(angle.toFloat())
                    }
                    if (!wasHomed && time > (0.5f / speedFactor)) {
                        wasHomed = true
                        shot.direction
                            .set(flagship.internalPosition.cpy().sub(shot.internalPosition))
                            .setLength(Shot.SPEED_SLOW * speedFactor)
                    }
                },
                enemyShotTexture
            )
        }
    }

    private fun spawnHomingEnemyShot(
        enemy: Enemy,
        maxAngularSpeed: Float = 0f,
        isAvailableAt: (time: Float) -> Boolean = { false }
    ) {
        enemyShots += Shot(
            enemy.internalPosition.cpy(),
            initialDirection = Vector2(
                flagship.internalPosition.cpy().sub(enemy.internalPosition)
            ).setLength(Shot.SPEED_SLOW),
            directionDt = { shot, time, delta ->
                if (isAvailableAt(time)) {
                    val currentAngle = shot.direction.angleDeg()
                    val desiredRotation = flagship.internalPosition.cpy().sub(shot.internalPosition).angleDeg().let {
                        // TODO: fix a nasty bug on break between 0 and 360
                        minAbs(it - currentAngle, it + 360f - currentAngle)
                    }.let {
                        minAbs(it, maxAngularSpeed * delta * sign(it))
                    }
                    shot.direction.setAngleDeg(currentAngle + desiredRotation)
                }
            },
            texture = enemyShotTexture
        )
    }

    private inline fun minAbs(a: Float, b: Float) =
        if (abs(a) > abs(b)) b else a

    private inline fun maxAbs(a: Float, b: Float) =
        if (abs(a) > abs(b)) a else b

    private fun spawnStandardEnemyShot(enemy: Enemy) {
        val isStraight = totalGameTime < 20f
        val isHoming = totalGameTime > 40f//Random.nextBoolean()
        var wasSetOnce = false
        enemyShots += Shot(
            enemy.internalPosition.cpy(),
            { shot, time, delta ->
                if ((isHoming && time < 1f) || !wasSetOnce) {
                    wasSetOnce = true
                    if (isStraight) {
                        shot.direction.set(-Shot.SPEED_SLOW, 0f)
                    } else {
                        shot.direction
                            .set(flagship.internalPosition.cpy().sub(shot.internalPosition))
                            .setLength(Shot.SPEED_SLOW)
                    }
                }
            },
            enemyShotTexture
        )
    }

    private fun spawnPowerUp(location: Vector2) {
        powerUps += Shot(
            location.cpy(),
            initialDirection = Vector2(-Shot.SPEED_POWER_UP, 0f),
            texture = powerUpTexture,
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
            if (!enemyShot.internalPosition.isOnScreen) {
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
                bombs += Shot(
                    flagship.internalPosition.cpy(),
                    { _, _, _ -> },
                    explosionTexture,
                    isRotating = false
                )
                flagship.startOver()
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

        viewport.apply(true)
        renderBackground()
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
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
            it.color = Color.WHITE
            it.rect(0.5f, 0.5f, viewport.worldWidth - 1f, viewport.worldHeight - 1f)
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
}
