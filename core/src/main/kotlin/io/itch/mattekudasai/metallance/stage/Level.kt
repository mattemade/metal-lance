package io.itch.mattekudasai.metallance.stage

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
    private val fadeMusicOut: (forTime: Float) -> Unit,
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
                metaRepeaters.removeAll { !it.update(delta) }
                repeaters.removeAll { !it.update(delta) }
                return
            }
            metaRepeaters.removeAll { !it.update(delta) }
            repeaters.removeAll { !it.update(delta) }
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

                    "fade" -> {
                        fadeMusicOut(split.getFloat(2))
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
            action = { counter, periodicTime, totalTime ->
                prepareSpawner(split, fromIndex = 3)
                totalTime <= repeatFor
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
        val stageRewards = mutableMapOf<Int, Char>()
        var currentStage = 0
        var updatePositionList = mutableListOf<Enemy.(time: Float) -> Any>()
        var updatePosition = resetPosition()
        var sequence = startSequence()
        var trajectoryIndex = currentIndex
        var from: Float = 0f
        var to: Float = Float.MAX_VALUE
        var cancelling = false
        var activePeriods = Stack<Triple<Float, Float, Boolean>>()
        var currentSplit = split
        var markedAsBoss = false
        while (trajectoryIndex < currentSplit.size) {
            val trajectory = currentSplit.getString(trajectoryIndex++).split(" ")
            val direction = trajectory.getString(1).toNormal()
            when (trajectory.getString(0)) {
                "\\" -> {
                    val line = lines[this.currentIndex++]
                    currentSplit = line.split("  ")
                    trajectoryIndex = 0
                    continue
                }

                "after" -> {
                    from += trajectory.getFloat(1)
                    to = Float.MAX_VALUE
                    cancelling = false
                }

                "cancelafter" -> {
                    from += trajectory.getFloat(1)
                    to = Float.MAX_VALUE
                    cancelling = true
                }

                "for" -> to = from + trajectory.getFloat(1)
                "(" -> {
                    // TODO: it does not work properly yet!!!
                    updatePosition += sequence
                    activePeriods.push(Triple(from, to, cancelling))
                    from = 0f
                    to = Float.MAX_VALUE
                    cancelling = false
                    sequence = startSequence()
                }

                ")" -> {
                    sequence = sequence.wrapSequence(to)
                    val lastActivePeriod = activePeriods.pop()
                    from = lastActivePeriod.first
                    to = lastActivePeriod.second
                    cancelling = lastActivePeriod.third
                    updatePosition += sequence.activeIn(from, to, cancelling)
                    sequence = startSequence()
                }

                "lin" -> sequence += linearTrajectory(direction, trajectory.getFloat(2)).activeIn(from, to, cancelling)
                "sin" -> sequence += sinTrajectory(
                    direction,
                    trajectory.getFloat(2),
                    trajectory.getFloat(3),
                    trajectory.getFloat(4)
                ).activeIn(from, to, cancelling)

                "cos" -> sequence += cosTrajectory(
                    direction,
                    trajectory.getFloat(2),
                    trajectory.getFloat(3),
                    trajectory.getFloat(4)
                ).activeIn(from, to, cancelling)

                "move" -> sequence += directedTrajectory(
                    Vector2(trajectory.getFloat(1) * getWorldWidth(), trajectory.getFloat(2) * getWorldHeight()),
                    to - from
                ).activeIn(from, to, cancelling)

                "shoot" -> {
                    sequence += changeShootingPattern(subscription, trajectory.getString(1)).activeIn(
                        from,
                        from + 0.125f, // just some not very low and not very high number, so the changing pattern could be called at least once
                        false
                    )
                }

                "stage" -> {
                    if (activePeriods.isNotEmpty()) {
                        throw IllegalStateException("Should not switch the stage from within the sequence")
                    }
                    stageRewards[currentStage++] = trajectory.getString(1).toReward(group.size).type
                    updatePosition += sequence.activeIn(from, to, cancelling)
                    updatePositionList += updatePosition
                    updatePosition = resetPosition()
                    sequence = startSequence()
                    from = 0f
                    to = Float.MAX_VALUE
                    cancelling = false
                }

                "boss" -> {
                    markedAsBoss = true
                }
            }
        }
        if (activePeriods.isNotEmpty()) {
            throw IllegalStateException("Should end the sequence before ending the pattern")
        }
        updatePosition += sequence.wrapSequence(to)//.activeIn(from, to, cancelling)
        updatePositionList += updatePosition

        val groupSizeLessOneFloat = (group.size - 1).toFloat()
        val sideFactorsSizeLessOne = sideFactors.size - 1
        repeaters += DelayedRepeater(
            nextDelay = { _, _ -> period },
            initialDelay = 0f,
            action = { counter, periodicTime, totalTime ->
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
                        updatePositionDt = updatePositionList,
                        initialHitPoints = health.first,
                        invincibilityPeriod = health.second,
                        onRemoved = {
                            subscription.unsubscribe(it)
                            spawnedEnemies--
                        },
                        onDefeat = {
                            if (--reward.condition <= 0) {
                                reward.type
                            } else {
                                null
                            }
                        },
                        onStageDefeat = {
                            stageRewards[it]
                        },
                        isBoss = markedAsBoss
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
            'G' -> 1 to 0f // baloon
            else -> 1 to 0f
        }

    private fun String.toNormal() = when (this) {
        "top" -> Align.top
        "bottom" -> Align.bottom
        "left" -> Align.left
        "right" -> Align.right
        else -> Align.right
    }

    private inline fun (Enemy.(time: Float) -> Any).activeIn(
        from: Float,
        to: Float,
        cancelling: Boolean
    ): (Enemy.(time: Float) -> Unit) =
        { time ->
            if (time >= from && (!cancelling || time <= to)) {
                this@activeIn(
                    if (time >= from) {
                        if (time < to) time - from else to - from
                    } else 0f
                )
            }
        }

    private inline operator fun (Enemy.(time: Float) -> Any).plus(crossinline another: Enemy.(time: Float) -> Any): Enemy.(time: Float) -> Unit =
        { time ->
            this@plus(time)
            another(time)
        }

    private fun startSequence(): Enemy.(time: Float) -> Any = { }

    private inline fun (Enemy.(time: Float) -> Any).wrapSequence(limit: Float): (Enemy.(time: Float) -> Unit) =
        { time ->
            var remainingTime = time
            while (remainingTime > 0f) {
                val actualTime = min(remainingTime, limit)
                this@wrapSequence(actualTime)
                remainingTime -= limit
            }
        }

    private fun resetPosition(): Enemy.(time: Float) -> Any = {
        internalPosition.set(initialPosition)
    }

    private fun linearTrajectory(direction: Int, speed: Float): Enemy.(time: Float) -> Unit = { time ->
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
    }

    private fun directedTrajectory(position: Vector2, toMinusFromTime: Float): Enemy.(time: Float) -> Unit = { time ->
        if (time >= toMinusFromTime) {
            internalPosition.set(position)
        } else {
            internalPosition.add(position.cpy().sub(internalPosition).scl(time / toMinusFromTime))
        }
    }


    private fun changeShootingPattern(
        subscription: ShootingPatternSubscription,
        pattern: String,
    ): Enemy.(time: Float) -> Unit {
        var changed = false
        var lastTime = Float.MAX_VALUE
        return { time ->
            if (time < lastTime) {
                changed = false
            }
            if (!changed) {
                subscription.onChanged(pattern)
                changed = true
            }
            lastTime = time
        }
    }


    private inline fun sinTrajectory(
        direction: Int,
        start: Float,
        speed: Float,
        amplitude: Float
    ): Enemy.(time: Float) -> Unit = { time ->
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
    }

    private inline fun cosTrajectory(
        direction: Int,
        start: Float,
        speed: Float,
        amplitude: Float
    ): Enemy.(time: Float) -> Unit = { time ->
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
        } else if (this[0] == 'c') { // angle speed to make a full circle in 8 beats and quater in 32b
            kotlin.math.PI.toFloat() / (4f * this.substring(1).toFloat() * 60f / musicTempo)
        } else if (this[0] == 'p') { // multiplier of pi
            kotlin.math.PI.toFloat() * this.substring(1).toFloat()
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
        val updatePositionDt: List<Enemy.(time: Float) -> Any>,
        val initialHitPoints: Int,
        val invincibilityPeriod: Float,
        val onRemoved: (Enemy) -> Unit,
        val onDefeat: () -> Char?,
        val onStageDefeat: (Int) -> Char?,
        val isBoss: Boolean,
    )

    private class Reward(
        val type: Char,
        var condition: Int
    )

    class ShootingPatternSubscription(val tempoProvider: () -> Float) {

        private var activeSubscriptions = mutableMapOf<Any, (Long) -> Unit>()
        private var lastKnownPattern: Long = -1
        fun onChanged(pattern: Long) {
            if (pattern != lastKnownPattern) {
                lastKnownPattern = pattern
                activeSubscriptions.values.forEach { it(pattern) }
            }
        }

        fun onChanged(code: String) {
            val pattern = code.toPatternInt()
            if (pattern != lastKnownPattern) {
                lastKnownPattern = pattern
                activeSubscriptions.values.forEach { it(pattern) }
            }
        }

        fun subscribe(key: Any, action: (Long) -> Unit) {
            activeSubscriptions += key to action
            if (lastKnownPattern > -1) {
                action.invoke(lastKnownPattern)
            }
        }

        fun unsubscribe(key: Any) {
            activeSubscriptions -= key
        }
    }
}

