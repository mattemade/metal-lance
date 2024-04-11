package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.player.Shot
import kotlin.math.abs
import kotlin.math.sign

class ShootingPattern(
    val initialDelay: Float,
    val nextDelay: (Float) -> Float,
    val shotTextureIndex: Int,
    val onShoot: (enemy: Enemy, flagshipPosition: Vector2, texture: Texture) -> List<Shot>
) {

    companion object {
        fun Int.toPattern(): ShootingPattern =
            when (this) {
                0 -> ShootingPattern(2f, { 2f }, 0) { enemy, flagshipPosition, texture ->
                    spawnHomingEnemyShot(enemy, flagshipPosition, texture)
                }

                1 -> ShootingPattern(1f, { 1f }, 0) { enemy, flagshipPosition, texture ->
                    spawnHomingEnemyShot(enemy, flagshipPosition, texture, 100f) { true }
                }

                2 -> ShootingPattern(1f, { 1f }, 0) { enemy, flagshipPosition, texture ->
                    spawnHomingEnemyShot(enemy, flagshipPosition, texture, 10f) { it < 3f }
                }

                3 -> ShootingPattern(0.61f, { 0.6f }, 0) { enemy, flagshipPosition, texture ->
                    spawnHomingEnemyShot(enemy, flagshipPosition, texture, 10f) { it < 1f }
                }

                else -> ShootingPattern(2f, { 2f }, 0) { enemy, _, texture ->
                    spawnHomingEnemyShot(enemy, enemy.initialPosition.cpy().add(-100f, 0f), texture)
                }
            }

        private fun spawnHomingEnemyShot(
            enemy: Enemy,
            flagshipPosition: Vector2,
            texture: Texture,
            maxAngularSpeed: Float = 0f,
            speed: Float = Shot.SPEED_SLOW,
            initialDirection: Vector2 = Vector2(
                flagshipPosition.cpy().sub(enemy.internalPosition)
            ).setLength(speed),
            isAvailableAt: (time: Float) -> Boolean = { false },
        ) = listOf(
            Shot(
                enemy.internalPosition.cpy(),
                initialDirection = initialDirection,
                directionDt = { shot, time, delta ->
                    if (isAvailableAt(time)) {
                        val currentAngle = shot.direction.angleDeg()
                        val desiredRotation = flagshipPosition.cpy().sub(shot.internalPosition).angleDeg().let {
                            minAbs(minAbs(it - currentAngle, it + 360f - currentAngle), it - 360f - currentAngle)
                        }.let {
                            minAbs(it, maxAngularSpeed * delta * sign(it))
                        }
                        shot.direction.setAngleDeg(currentAngle + desiredRotation)
                    }
                },
                texture = texture,
                timeToLive = 3f,
            )
        )

        /*
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
    }*/

        private inline fun minAbs(a: Float, b: Float) =
            if (abs(a) > abs(b)) b else a
    }

}
