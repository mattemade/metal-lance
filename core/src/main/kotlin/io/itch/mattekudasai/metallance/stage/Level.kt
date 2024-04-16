package io.itch.mattekudasai.metallance.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import io.itch.mattekudasai.metallance.enemy.DelayedRepeater
import io.itch.mattekudasai.metallance.enemy.Enemy
import io.itch.mattekudasai.metallance.enemy.ShootingPattern.Companion.toPatternInt
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Level(
    scriptFile: FileHandle,
    private val setBackground: (String) -> Unit,
    private val showText: (TextConfiguration) -> Unit,
    private val spawnEnemy: (EnemyConfiguration) -> Unit,
    private val setRenderMode: (mode: Int, stage: Int) -> Unit,
    private val setTint: (tint: Color) -> Unit,
    private val playMusic: (assetPath: String, volume: Float) -> Unit,
    private val winningCondition: (condition: String, counter: Int) -> Unit,
    private val endSequence: () -> Unit,
    private val getWorldWidth: () -> Float,
    private val getWorldHeight: () -> Float,
) {

    private val lines = scriptFile.reader().readLines()
    private val repeaters = mutableListOf<DelayedRepeater>()
    private val metaRepeaters = mutableListOf<DelayedRepeater>()
    private var currentIndex = 0
    private var waitTime = 0f
    private var spawnedEnemies: Int = 0
    private var waitingDefeated = false
    private var nextValidIndex = 0
    var musicTempo = 120f
        private set
    private var internalTimer = 0f

    fun update(delta: Float) {
        internalTimer += delta
        if (waitingDefeated) {
            if (spawnedEnemies == 0) {
                waitingDefeated = false
                if (waitTime > 0f) {
                    waitTime -= internalTimer % waitTime
                }
                /* metaRepeaters.removeAll { !it.update(delta) }
                 repeaters.removeAll { !it.update(delta) }*/
            } else {
                return
            }
            return
        }
        waitTime -= delta
        while (waitTime < 0f && currentIndex < lines.size) {
            if (currentIndex < nextValidIndex) {
                currentIndex++
                continue
            }
            val line = lines[currentIndex++]
            val split = line.split("  ")
            when (split.getString(0)) {
                "#" -> {}
                "goto" -> nextValidIndex = split.getInt(1) - 1
                "goal" -> winningCondition(split.getString(1), split.getInt(2))
                "setting" -> setBackground(split.getString(1))
                "mode" -> setRenderMode(split.getInt(1), split.getInt(2))
                "music" -> when (split.getString(1)) {
                    "play" -> {
                        playMusic(split.getString(2), split.getFloat(3))
                        musicTempo = split.getFloat(4, 120f)
                    }
                }

                "tint" -> setTint(Color(split.getFloat(1), split.getFloat(2), split.getFloat(3), 1f))
                "text" -> showText(
                    TextConfiguration(
                        text = split.getString(1).split("\\"),
                        positionX = split.getFloat(2),
                        positionY = split.getFloat(3)
                    )
                )

                "spawn" -> prepareSpawner(split)
                "repeat" -> prepareSpawnerRepeater(split)
                "wait" -> waitTime += split.getFloat(1)
                "waitdefeated" -> {
                    waitTime = split.getFloat(1) // this id MOD for internal timer, not the time really!!
                    waitingDefeated = true
                }
                "end" -> endSequence()
            }
        }

        metaRepeaters.removeAll { !it.update(delta) }
        repeaters.removeAll { !it.update(delta) }
    }

    private fun prepareSpawnerRepeater(split: List<String>) {
        val delayBetweenRepeats = split.getFloat(1)
        val repeatFor = split.getFloat(2)
        val withinTime = split.getFloat(3)
        metaRepeaters += DelayedRepeater(
            nextDelay = { _, _ -> withinTime + delayBetweenRepeats },
            initialDelay = 0f,
            action = { counter, time ->
                prepareSpawner(split, fromIndex = 3)
                time <= repeatFor
            }
        )
    }

    private fun prepareSpawner(split: List<String>, fromIndex: Int = 1) {
        var currentIndex = fromIndex
        val withinTime = split.getFloat(currentIndex++)
        val group = split.getString(currentIndex++).split(" ")
        val reward = split.getString(currentIndex++).toReward(group.size)
        val period = withinTime / group.size
        val spawnSide = split.getString(currentIndex++).toNormal()
        val sideFactors = split.getString(currentIndex++).split(" ").map {
            it.parseToFloat()
        }
        val subscription = ShootingPatternSubscription { musicTempo }
        var updatePosition = resetPosition()
        var sequence = startSequence()
        var trajectoryIndex = currentIndex
        var from: Float = 0f
        var to: Float = Float.MAX_VALUE
        var activePeriods = Stack<Pair<Float, Float>>()
        while (trajectoryIndex < split.size) {
            val trajectory = split.getString(trajectoryIndex++).split(" ")
            val direction = trajectory.getString(1).toNormal()
            when (trajectory.getString(0)) {
                "after" -> {
                    from += trajectory.getFloat(1)
                    to = Float.MAX_VALUE
                }

                "for" -> to = from + trajectory.getFloat(1)
                "(" -> {
                    // TODO: it does not work properly yet!!!
                    updatePosition += sequence
                    activePeriods.push(from to to)
                    from = 0f
                    to = Float.MAX_VALUE
                    sequence = startSequence()
                }

                ")" -> {
                    sequence = sequence.wrapSequence(from, to)
                    val lastActivePeriod = activePeriods.pop()
                    from = lastActivePeriod.first
                    to = lastActivePeriod.second
                    updatePosition += sequence.activeIn(from, to)
                    sequence = startSequence()
                }

                "lin" -> sequence += linearTrajectory(direction, trajectory.getFloat(2)).activeIn(from, to)
                "sin" -> sequence += sinTrajectory(
                    direction,
                    trajectory.getFloat(2),
                    trajectory.getFloat(3),
                    trajectory.getFloat(4)
                ).activeIn(from, to)
                "cos" -> sequence += cosTrajectory(
                    direction,
                    trajectory.getFloat(2),
                    trajectory.getFloat(3),
                    trajectory.getFloat(4)
                ).activeIn(from, to)

                "move" -> sequence += directedTrajectory(
                    Vector2(trajectory.getFloat(1) * getWorldWidth(), trajectory.getFloat(2) * getWorldHeight()),
                    to - from
                ).activeIn(from, to)

                "shoot" -> {
                    // TODO: it does not work properly in sequences!!!
                    sequence += changeShootingPattern(subscription, trajectory.getString(1), from)
                }
            }
        }
        updatePosition += sequence

        val groupSizeLessOneFloat = (group.size - 1).toFloat()
        val sideFactorsSizeLessOne = sideFactors.size - 1
        repeaters += DelayedRepeater(
            nextDelay = { _, _ -> period },
            initialDelay = 0f,
            action = { counter, time ->
                val indexInGroup = counter - 1
                val enemyLine = group[indexInGroup]
                val sideFactorPosition = indexInGroup / groupSizeLessOneFloat * sideFactorsSizeLessOne
                val sideFactorIndex = sideFactorPosition.toInt()
                val sideFactor = if (sideFactorIndex == sideFactorsSizeLessOne) {
                    sideFactors[sideFactorIndex]
                } else {
                    val distance = sideFactorPosition - sideFactorIndex
                    val leftSide = sideFactors[sideFactorIndex]
                    val rightSide = sideFactors[sideFactorIndex + 1]
                    leftSide + (rightSide - leftSide) * distance
                }
                val health = enemyLine[0].health()
                spawnedEnemies++
                spawnEnemy(
                    EnemyConfiguration(
                        enemyType = enemyLine[0] - 'A',
                        shootingPatternSubscription = subscription,
                        spawnSide = spawnSide,
                        spawnSideFactor = sideFactor,
                        updatePositionDt = updatePosition,
                        initialHitPoints = health.first,
                        invincibilityPeriod = health.second,
                        onRemoved = {
                            spawnedEnemies--
                        },
                        onDefeat = {
                            if (--reward.condition <= 0) {
                                reward.type
                            } else {
                                null
                            }
                        }
                    )
                )
                val substring = enemyLine.substring(1)
                if (substring[0].isLetter()) {
                    subscription.onChanged(substring)
                } else {
                    subscription.onChanged(substring.toLong())
                }


                counter < group.size
            },
        )
    }

    private fun Char.health() =
        when (this) {
            'A' -> 1 to 0f
            'B' -> 1 to 0f
            'C' -> 2 to 1f
            'D' -> 3 to 1f
            'E' -> 30 to 0f // level 1 boss
            'F' -> 50 to 0f // level 2 boss
            else -> 1 to 0f
        }

    private fun String.toNormal() = when (this) {
        "top" -> Align.top
        "bottom" -> Align.bottom
        "left" -> Align.left
        "right" -> Align.right
        else -> Align.right
    }

    private inline fun (Enemy.(time: Float) -> Any).activeIn(from: Float, to: Float): (Enemy.(time: Float) -> ActiveInMarker) =
        { time ->
            this@activeIn(
                if (time >= from) {
                    if (time < to) time - from else to - from
                } else 0f
            )
            ActiveInMarker
        }

    private inline operator fun (Enemy.(time: Float) -> Any).plus(crossinline another: Enemy.(time: Float) -> Any): Enemy.(time: Float) -> PlusMarker =
        { time ->
            this@plus(time)
            another(time)
            PlusMarker
        }

    private fun startSequence(): Enemy.(time: Float) -> Any = { }

    private inline fun (Enemy.(time: Float) -> Any).wrapSequence(from: Float, to: Float): (Enemy.(time: Float) -> WrapSeqMarker) =
        { time ->
            var remainingTime = time
            while (remainingTime > 0f) {
                val actualTime = min(remainingTime, to)
                if (actualTime >= from) {
                    this@wrapSequence(actualTime)
                }
                remainingTime -= to
            }
            WrapSeqMarker
        }

    private fun resetPosition(): Enemy.(time: Float) -> Any = {
        internalPosition.set(initialPosition)
    }

    private fun linearTrajectory(direction: Int, speed: Float): Enemy.(time: Float) -> LinMarker = { time ->
        internalPosition.add(
            when {
                Align.isRight(direction) -> time * speed
                Align.isLeft(direction) -> -time * speed
                else -> 0f
            },
            when {
                Align.isTop(direction) -> time * speed
                Align.isBottom(direction) -> -time * speed
                else -> 0f
            }
        )
        LinMarker
    }

    private fun directedTrajectory(position: Vector2, toMinusFromTime: Float): Enemy.(time: Float) -> DirectedMarker = { time ->
        if (time >= toMinusFromTime) {
            internalPosition.set(position)
        } else {
            internalPosition.add(position.cpy().sub(internalPosition).scl(time / toMinusFromTime))
        }
        DirectedMarker
    }

    object DirectedMarker
    object SinMarker
    object CosMarker
    object PlusMarker
    object LinMarker
    object WrapSeqMarker
    object ActiveInMarker


    private fun changeShootingPattern(
        subscription: ShootingPatternSubscription,
        pattern: String,
        from: Float
    ): Enemy.(time: Float) -> Unit {
        var changed = false
        return { time ->
            if (time < from) {
                changed = false
            } else if (!changed) {
                subscription.onChanged(pattern)
                changed = true
            }
        }
    }


    private inline fun sinTrajectory(
        direction: Int,
        start: Float,
        speed: Float,
        amplitude: Float
    ): Enemy.(time: Float) -> SinMarker = { time ->
        internalPosition.add(
            when {
                Align.isRight(direction) -> sin(start + time * speed) * amplitude
                Align.isLeft(direction) -> -sin(start + time * speed) * amplitude
                else -> 0f
            },
            when {
                Align.isTop(direction) -> sin(start + time * speed) * amplitude
                Align.isBottom(direction) -> -sin(start + time * speed) * amplitude
                else -> 0f
            }
        )
        SinMarker
    }

    private inline fun cosTrajectory(
        direction: Int,
        start: Float,
        speed: Float,
        amplitude: Float
    ): Enemy.(time: Float) -> CosMarker = { time ->
        internalPosition.add(
            when {
                Align.isRight(direction) -> cos(start + time * speed) * amplitude
                Align.isLeft(direction) -> -cos(start + time * speed) * amplitude
                else -> 0f
            },
            when {
                Align.isTop(direction) -> cos(start + time * speed) * amplitude
                Align.isBottom(direction) -> -cos(start + time * speed) * amplitude
                else -> 0f
            }
        )
        CosMarker
    }

    private fun List<String>.getString(index: Int, defaultValue: String = ""): String =
        if (index >= size) {
            defaultValue
        } else {
            get(index)
        }

    private fun List<String>.getInt(index: Int, defaultValue: Int = 0): Int =
        if (index >= size) {
            defaultValue
        } else {
            get(index).toInt()
        }

    private fun List<String>.getFloat(index: Int, defaultValue: Float = 0f): Float =
        if (index >= size) {
            defaultValue
        } else {
            get(index).parseToFloat()
        }

    private fun String.parseToFloat() =
        if (this[0] == 'R') {
            val parts = this.substring(1).split("-")
            val from = parts.getFloat(0)
            val to = parts.getFloat(1)
            from + Random.nextFloat() * to
        } else if (this[0] == 'b') {
            this.substring(1).toFloat() * 60f / musicTempo
        } else {
            this.toFloat()
        }

    private fun String.toReward(groupSize: Int): Reward =
        Reward(
            type = this[0],
            condition = when (val condition = substring(1)) {
                "all" -> groupSize
                "each" -> 1
                "one" -> Int.MAX_VALUE // hacky check for "none"
                else -> condition.toInt()
            }
        )

    class TextConfiguration(
        val text: List<String>,
        val positionX: Float,
        val positionY: Float,
    )

    class EnemyConfiguration(
        val enemyType: Int,
        val shootingPatternSubscription: ShootingPatternSubscription,
        val spawnSide: Int = Align.right, // e.g. "where does it come from?"
        val spawnSideFactor: Float = 0.5f, // e.g. "how far from 0 to SIDE_LENGTH does it come from?"
        val updatePositionDt: Enemy.(time: Float) -> Any,
        val initialHitPoints: Int,
        val invincibilityPeriod: Float,
        val onRemoved: () -> Unit,
        val onDefeat: () -> Char?
    )

    private class Reward(
        val type: Char,
        var condition: Int
    )

    class ShootingPatternSubscription(val tempoProvider: () -> Float) {

        private var activeSubscription: ((Long) -> Unit)? = null
        private var lastKnownPattern: Long = -1
        fun onChanged(pattern: Long) {
            if (pattern != lastKnownPattern) {
                lastKnownPattern = pattern
                activeSubscription?.invoke(pattern)
            }
        }

        fun onChanged(code: String) {
            val pattern = code.toPatternInt()
            if (pattern != lastKnownPattern) {
                lastKnownPattern = pattern
                activeSubscription?.invoke(pattern)
            }
        }

        fun subscribe(action: (Long) -> Unit) {
            activeSubscription = action
            if (lastKnownPattern > -1) {
                action.invoke(lastKnownPattern)
            }
        }
    }
}

