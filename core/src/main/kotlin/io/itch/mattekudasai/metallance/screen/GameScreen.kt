package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.enemy.DelayedRepeater
import io.itch.mattekudasai.metallance.enemy.Enemy
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.player.Shot
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
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class GameScreen : KtxScreen, KtxInputAdapter, Disposing by Self() {

    private val flagship: Flagship by remember {
        Flagship(
            viewport.worldWidth,
            viewport.worldHeight,
            explosionTexture,
            ::shoot
        )
    }
    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer().apply { color = Color.WHITE } }
    private val filledShapeRenderer: ShapeRenderer by remember { ShapeRenderer().apply { color = Color.BLACK } }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val enemies = mutableDisposableListOf<Enemy>(onDisposed = ::forget).autoDisposing()
    private val enemyShots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val powerUps = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val bombs = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()

    // TODO: pack all the textures in one 2048x2048 atlas to avoid constant rebinding
    private val shotTexture: Texture by remember { Texture("shot.png".overridable) }
    private val enemyTexture: Texture by remember { Texture("saucer.png".overridable) }
    private val explosionTexture: Texture by remember { Texture("explosion.png".overridable) }
    private val enemyShotTexture: Texture by remember { Texture("wave.png".overridable) }
    private val powerUpTexture: Texture by remember { Texture("power.png".overridable) }
    private val enemySpawner =
        DelayedRepeater(nextDelay = { /*if (true) 100f else */max(0.1f, 2f - totalGameTime / 45f) }) {
            val horizontalPosition = 20f + 200f * Random.nextFloat()
            val startOscillation = Random.nextFloat() * Math.PI.toFloat()
            val oscillationSpeed = Random.nextFloat() * 6f
            enemies += Enemy(
                texture = enemyTexture,
                explosionTexture = explosionTexture,
                positionDt = { position, time ->
                    position.set(
                        260f - time * 50f,
                        horizontalPosition + sin(startOscillation + time * oscillationSpeed) * 20f
                    )
                },
                nextShootingDelay = { max(0.1f, 2f - totalGameTime / 60f) },
                shot = if (Random.nextBoolean()) ::spawnStandardEnemyShot else ::spawnSunEnemyShot
            )
        }

    val textDrawer: MonoSpaceTextDrawer by remember {
        MonoSpaceTextDrawer(
            fontFileName = "font_white.png",
            alphabet = ('A'..'Z').joinToString(separator = ""),
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
        music.play()
        music.isLooping = true
    }

    private fun shoot() {
        shots += Shot(
            flagship.internalPosition.cpy(),
            { shot, _ -> shot.direction.set(Shot.SPEED_FAST, 0f) },
            shotTexture
        )
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
                { shot, time ->
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

    private fun spawnStandardEnemyShot(enemy: Enemy) {
        val isStraight = totalGameTime < 20f
        val isHoming = totalGameTime > 40f//Random.nextBoolean()
        var wasSetOnce = false
        enemyShots += Shot(
            enemy.internalPosition.cpy(),
            { shot, time ->
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
        var wasSetOnce = false
        powerUps += Shot(
            location.cpy(),
            { shot, time ->
                if (!wasSetOnce) {
                    shot.direction.set(-Shot.SPEED_POWER_UP, 0f)
                    wasSetOnce = true
                }
            },
            powerUpTexture,
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

        flagship.update(delta)
        enemySpawner.update(delta)
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
                return@removeAll true
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
                    { _, _ -> },
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
        batch.use(camera) { batch ->
            enemyShots.forEach { it.draw(batch) }
        }
        if (bombs.isNotEmpty()) {
            filledShapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) { renderer ->
                bombs.forEach {
                    renderer.circle(it.internalPosition.x, it.internalPosition.y, it.internalTimer * 200f)
                }
            }
            shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) { renderer ->
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
