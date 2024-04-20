package io.itch.mattekudasai.metallance.enemy

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.`object`.Shot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.random.Random

class ShootingPattern(
    val initialDelay: Float,
    val nextDelay: (Int, Float) -> Float,
    val shotTextureIndex: Int,
    val onShoot: (enemy: Enemy, flagshipPosition: Vector2, texture: Texture) -> List<Shot>
) {

    companion object {
        private const val MIN_DELAY = 0.01f
        private const val MAX_DELAY = 2f
        private const val MIN_ANGLE_SPEED = 0f
        private const val MAX_ANGLE_SPEED = 360f
        private const val MIN_SPEED = 10f
        private const val MAX_SPEED = 200f
        private const val MIN_START_HOMING_DISTANCE = 0f
        private const val MAX_START_HOMING_DISTANCE = 40f
        private const val MIN_HOMING_TIME = 0.04f // just to have at least one activation frame
        private const val MAX_HOMING_TIME = 10f
        private const val MIN_TIME_TO_LIVE_FACTOR = 0.1f
        private const val MAX_TIME_TO_LIVE_FACTOR = 3f

        fun String.toPatternInt(): Long {
            var result = 0L

            this.forEachIndexed { index, c ->
                if (index == 0) {
                    result =
                        when (c) {
                            'A' -> 0 // does not shoot
                            'B' -> 1492000119000L // slowly shoots towards the player
                            'C' -> 1492000009000L // slowly shoots forward
                            'D' -> 1174000119000L // faster shooting towards the player
                            'E' -> 1172002119000L // slowly shooting towards the player, slow and short homing
                            'F' -> 1173003939000L // faster shooting towards the player, slow starting better and longer homing
                            'G' -> 1174003139000L // faster shooting towards the player, better and longer homing
                            'H' -> 1555000001000L // quick short spree forward
                            'J' -> 5575109990000L // ???
                            'K' -> 1035009119000L // quick long spree towards the player
                            'L' -> 1509000003200L // slow lazer directed to movement
                            'S' -> 1502000003300L // cloud of steam
                            //     cdpsggahhtiii
                            'Z' -> 1591000003200L // cloud of steam
                            else -> 0
                        }.reverse()
                } else {
                    result =
                        when (c) {
                            in '2'..'9' -> result.replace(0) { c - '0' }
                            'F' -> result.replace(2) { max(0, this - 1) } // faster, shorter period
                            'G' -> result.replace(2) { min(9, this + 1) } // grubby, longer period
                            'D' -> result.replace(1) { 0 } // don't wait before shooting
                            'S' -> result.replace(4) { 2 } // spread
                            'W' -> result.replace(4) { 5 } // widespread
                            'R' -> result.replace(4) { 9 } // round
                            'Q' -> result.replace(5) { 5 } // queued group of half period
                            'T' -> result.replace(5) { 9 } // queued group during full period
                            'H' -> {
                                result = result.replace(7) { 1 }.reverse()
                                result.replace(8) { 1 }
                            } // homing initially
                            'L' -> result.replace(8) { max(0, this - 1) } // lesser homing period
                            'N' -> result.replace(9) { 9 } // neverending
                            'M' -> result.replace(9) { max(0, this - 1) } // mock, die faster
                            'K' -> result.replace(3) { min(9, this + 1) } // kuickier, moving faster
                            'P' -> result.replace(3) { max(0, this - 1) } // putty, moving slower
                            'B' -> {
                                result = result.replace(10) { 8 }.reverse()
                                result = result.replace(11) { 2 }.reverse()
                                result.replace(12) { 1 }
                            } // bomb
                            'Z' -> {
                                result = result.replace(10) { 9 }.reverse()
                                result = result.replace(11) { 2 }.reverse()
                                result.replace(12) { 1 }
                            } // boss shield out of bombs
                            'X' -> {
                                result = result.replace(10) { 0 }.reverse()
                                result = result.replace(11) { 3 }.reverse()
                                result.replace(12) { 1 }
                            } // boss circle of bombs
                            else -> result
                        }.reverse()
                }
            }

            return result
        }

        private fun Long.replace(place: Int, by: Int.() -> Int): Long {
            var result = 0L
            var input = this
            var placeRemains = place
            while (placeRemains >= 0 || input > 0L) {
                result *= 10
                result += if (placeRemains == 0) (input % 10).toInt().by().toLong() else input % 10
                input /= 10
                placeRemains--
            }
            return result
        }

        private fun Long.reverse(): Long {
            var result = 0L
            var input = this
            while (input != 0L) {
                result *= 10
                result += input % 10
                input /= 10
            }
            return result
        }

        private fun Long.inRange(maxPlusOne: Int, from: Float, to: Float): Float =
            from + this * (to - from) / (maxPlusOne - 1)

        fun Long.toPattern(worldWidth: Float, tempoProvider: () -> Float): ShootingPattern {
            var remaining = this
            val countPerShot = remaining % 10; remaining /= 10
            val initialDelay = remaining % 10; remaining /= 10
            val period = remaining % 10; remaining /= 10
            val speed = remaining % 10; remaining /= 10
            val groupingRule = remaining % 10; remaining /= 10 // 0 - all in one place, 1-9 - spread in different angles
            val groupPeriod = remaining % 10; remaining /= 10
            val angleSpeed = remaining % 10; remaining /= 10
            val homingFrom = remaining % 10; remaining /= 10
            val homingFor = remaining % 10; remaining /= 10
            val timeToLive = remaining % 10; remaining /= 10
            val textureIndex = remaining

            val secondPerBeat = 60f / tempoProvider()
            val initialDelayFloat = secondPerBeat * 2.0.pow(initialDelay - 5.0).toFloat()
            val periodFloat = /*if (period == 9L) 0.001f else*/ secondPerBeat * 2.0.pow(period - 7.0).toFloat()
            val groupPeriodFloat = groupPeriod.inRange(10, 0f, periodFloat / countPerShot)
            val shotSpeedFloat = if (speed == 9L) 400f else speed.inRange(10, MIN_SPEED, MAX_SPEED)
            val angleSpeedFloat = angleSpeed.inRange(10, MIN_ANGLE_SPEED, MAX_ANGLE_SPEED)
            val shootingInAngleRange = 360f * groupingRule / 9f
            val anglePerShot = shootingInAngleRange / (countPerShot)
            val startAngle = (shootingInAngleRange - anglePerShot) / 2f
            val homingFromFloat =
                homingFrom.inRange(10, MIN_START_HOMING_DISTANCE, MAX_START_HOMING_DISTANCE) / shotSpeedFloat
            val homingToFloat = if (homingFor == 0L) {
                Float.MAX_VALUE
            } else {
                homingFromFloat + (homingFor - 1).inRange(9, MIN_HOMING_TIME, MAX_HOMING_TIME)
            }
            val timeToLiveFloat =
                worldWidth / shotSpeedFloat * timeToLive.inRange(10, MIN_TIME_TO_LIVE_FACTOR, MAX_TIME_TO_LIVE_FACTOR)

            var shotCounter = 0

            return ShootingPattern(
                if (countPerShot == 0L) Float.MAX_VALUE else initialDelayFloat,
                { count, _ ->
                    if (count % countPerShot == 0L) periodFloat - (groupPeriodFloat * countPerShot) else groupPeriodFloat
                },
                textureIndex.toInt(),
            ) { enemy, flagshipPosition, texture ->
                if (textureIndex == 129L) {
                    val rotation =
                    if (startAngle != 0f && anglePerShot != 0f) {
                        -startAngle + anglePerShot * shotCounter
                    } else 0f
                    shotCounter = (shotCounter + 1) % countPerShot.toInt()
                    return@ShootingPattern spawnCirclingShot(enemy, texture, rotation, timeToLiveFloat, restricted = true)
                } else if (textureIndex == 130L) {
                    val rotation =
                        if (startAngle != 0f && anglePerShot != 0f) {
                            -startAngle + anglePerShot * shotCounter
                        } else 0f
                    shotCounter = (shotCounter + 1) % countPerShot.toInt()
                    return@ShootingPattern spawnCirclingShot(enemy, texture, rotation, timeToLiveFloat, restricted = false)
                }
                spawnHomingEnemyShot(
                    enemy,
                    flagshipPosition,
                    texture,
                    angleSpeedFloat,
                    initialDirection = {
                        val initialVector = if (homingFor == 0L) {
                            enemy.internalPosition.cpy().sub(enemy.previousPosition).let {
                                if (it.len() == 0f) {
                                    it.set(1f - Random.nextFloat() * 2f, 1f - Random.nextFloat() * 2f) // just some arbitrary values
                                } else {
                                    it
                                }
                            }
                        } else {
                            flagshipPosition.cpy().sub(enemy.internalPosition)
                        }
                        val result = initialVector.setLength(shotSpeedFloat)
                        if (startAngle != 0f && anglePerShot != 0f) {
                            result.rotateDeg(-startAngle + anglePerShot * shotCounter)
                        }
                        shotCounter = (shotCounter + 1) % countPerShot.toInt()
                        return@spawnHomingEnemyShot result
                    },
                    isAvailableAt = { time ->
                        time > homingFromFloat && time <= homingToFloat
                    },
                    timeToLive = timeToLiveFloat,
                    alpha = if (textureIndex == 3L) 0.5f else 1f,
                )
            }
        }

        private fun spawnHomingEnemyShot(
            enemy: Enemy,
            flagshipPosition: Vector2,
            texture: Texture,
            maxAngularSpeed: Float = 0f,
            initialDirection: () -> Vector2 = {
                Vector2(flagshipPosition.cpy().sub(enemy.internalPosition)).setLength(
                    Shot.SPEED_SLOW
                )
            },
            isAvailableAt: (time: Float) -> Boolean = { false },
            timeToLive: Float,
            alpha: Float = 1f,
        ) = listOf(
            Shot(
                enemy.internalPosition.cpy(),
                initialDirection = initialDirection(),
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
                timeToLive = timeToLive,
            ).apply {
                setAlpha(alpha)
            }
        )

        private val tempPosition = Vector2()

        private fun spawnCirclingShot(
            enemy: Enemy,
            texture: Texture,
            rotation: Float,
            timeToLive: Float,
            restricted: Boolean,
        ) = listOf(
            Shot(
                enemy.internalPosition.cpy(),
                initialDirection = Vector2(0f, 0f),
                directionDt = { shot, time, delta ->
                    if (enemy.isAlive && !shot.markedForRemoval) {
                        val length = min(if (restricted) enemy.width * 1.5f else 10000f, time*40f)
                        tempPosition.set(length, 0f).rotateDeg(rotation + time*100f)
                        shot.internalPosition.set(if (restricted) enemy.internalPosition else shot.initialPosition).add(tempPosition)
                    } else {
                        shot.internalPosition.set(-100f, -100f)
                    }
                },
                texture = texture,
                timeToLive = timeToLive,
            )
        )

        private inline fun minAbs(a: Float, b: Float) =
            if (abs(a) > abs(b)) b else a
    }


}
