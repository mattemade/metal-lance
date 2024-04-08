package io.itch.mattekudasai.metallance.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import io.itch.mattekudasai.metallance.enemy.DelayedRepeater
import io.itch.mattekudasai.metallance.enemy.Enemy
import io.itch.mattekudasai.metallance.player.Flagship
import io.itch.mattekudasai.metallance.player.Shot
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.disposing.mutableDisposableListOf
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.graphics.use
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
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(0f, 0f, camera)
    private val shots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()
    private val enemies = mutableDisposableListOf<Enemy>(onDisposed = ::forget).autoDisposing()
    private val enemyShots = mutableDisposableListOf<Shot>(onDisposed = ::forget).autoDisposing()

    // TODO: pack all the textures in one 2048x2048 atlas to avoid constant rebinding
    private val shotTexture: Texture by remember { Texture("shot.png") }
    private val enemyTexture: Texture by remember { Texture("saucer.png") }
    private val explosionTexture: Texture by remember { Texture("explosion.png") }
    private val enemyShotTexture: Texture by remember { Texture("wave.png") }
    private val enemySpawner = DelayedRepeater(initialDelay = 2f, repeatPeriod = 1f) {
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
            initialShootingDelay = 0.5f,
            shootingPeriod = 1f,
            shot = ::enemyShoot
        )
    }

    init {
        Gdx.input.inputProcessor = this
    }

    private fun shoot() {
        shots += Shot(
            flagship.internalPosition.cpy(),
            { shot, _ -> shot.direction.set(Shot.SPEED_FAST, 0f) },
            shotTexture
        )
    }

    private fun enemyShoot(enemy: Enemy) {
        val isStraight = totalGameTime < 5f
        val isHoming = totalGameTime > 10f//Random.nextBoolean()
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

    private val Vector2.isOnScreen: Boolean
        get() = x > -10f && x < (viewport.worldWidth + 10f) && y > -10f && y < (viewport.worldHeight + 10f)

    private var fpsTimer = 0f
    private var currentFrames = 0
    private var totalTime = 0f
    private var frameCounter = 0

    private var totalGameTime = 0f

    private var evenFrame = false

    override fun render(delta: Float) {
        evenFrame = !evenFrame
        totalGameTime += delta
        fpsTimer += delta
        frameCounter++
        currentFrames++
        totalTime += delta

        while (fpsTimer > 1f) {
            Gdx.app.debug("FPS", "${currentFrames}")
            Gdx.app.debug("AvgFPS", "${frameCounter / totalTime}")
            fpsTimer -= 1f
            currentFrames = 0
        }
        clearScreen(red = 0f, green = 0f, blue = 0f)

        flagship.update(delta)
        enemySpawner.update(delta)
        enemies.removeAll {
            it.update(delta)
            if (it.shouldBeRemoved || !it.internalPosition.isOnScreen) {
                return@removeAll true
            }
            // flagship collision check
            false
        }
        enemyShots.removeAll {
            it.update(delta)
            if (!it.internalPosition.isOnScreen) {
                return@removeAll true
            }
            // flagship collision check
            val flagshipHit = flagship.isAlive && !flagship.isInvincible && it.hits(flagship.internalPosition, 1.5f)
            if (flagshipHit) {
                flagship.explode()
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
                    break
                }
            }
            removeShot
        }



        viewport.apply(true)
        batch.use(camera) { batch ->
            enemyShots.forEach { it.draw(batch) }
            enemies.forEach { it.draw(batch) }
            shots.forEach { it.draw(batch) }
            if (!flagship.isInvincible || evenFrame) {
                flagship.draw(batch)
            }
        }
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
