package io.itch.mattekudasai.metallance.stage

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.itch.mattekudasai.metallance.enemy.DelayedRepeater
import io.itch.mattekudasai.metallance.enemy.Enemy
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
) {

    private val lines = scriptFile.reader().readLines()
    private val repeaters = mutableListOf<DelayedRepeater>()
    private val eternalRepeaters = mutableListOf<DelayedRepeater>()
    private var currentIndex = 0
    private var waitTime = 0f
    private var nextValidIndex = 0

    fun update(delta: Float) {
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
                    "play" -> playMusic(split.getString(2), split.getFloat(3))
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
                "repeat" -> prepareEternalSpawner(split)
                "wait" -> waitTime += split.getFloat(1)
                "end" -> endSequence()
            }
        }

        eternalRepeaters.forEach { it.update(delta) }
        repeaters.removeAll { !it.update(delta) }
    }

    private fun prepareEternalSpawner(split: List<String>) {
        val delayBetweenRepeats = split.getFloat(1)
        val withinTime = split.getFloat(2)
        eternalRepeaters += DelayedRepeater(
            nextDelay = { _, _ -> withinTime + delayBetweenRepeats },
            initialDelay = 0f,
            action = { counter, time ->
                prepareSpawner(split, fromIndex = 2)
                true
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
        val sideFactors = split.getString(currentIndex++).split(" ").map { it.toFloat() }
        var updatePosition = resetPosition()
        var trajectoryIndex = currentIndex
        while (trajectoryIndex < split.size) {
            val trajectory = split.getString(trajectoryIndex++).split(" ")
            val direction = trajectory.getString(1).toNormal()
            when (trajectory.getString(0)) {
                "lin" -> updatePosition += linearTrajectory(direction, trajectory.getFloat(2))
                "sin" -> updatePosition += sinTrajectory(
                    direction,
                    trajectory.getFloat(2),
                    trajectory.getFloat(3),
                    trajectory.getFloat(4)
                )
            }
        }

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
                spawnEnemy(
                    EnemyConfiguration(
                        enemyType = enemyLine[0] - 'A',
                        shootingPattern = enemyLine.substring(1).toInt(),
                        spawnSide = spawnSide,
                        spawnSideFactor = sideFactor,
                        updatePositionDt = updatePosition,
                        initialHitPoints = health.first,
                        invincibilityPeriod = health.second,
                        onDefeat = {
                            if (--reward.condition <= 0) {
                                reward.type
                            } else {
                                null
                            }
                        }
                    )
                )
                counter < group.size
            },
        )
    }

    private fun Char.health() =
        when (this) {
            'B' -> 1 to 0f
            'C' -> 2 to 0f
            'D' -> 3 to 0.5f
            'E' -> 10 to 0.1f
            else -> 1 to 0f
        }

    private fun String.toNormal() = when (this) {
        "top" -> Align.top
        "bottom" -> Align.bottom
        "left" -> Align.left
        "right" -> Align.right
        else -> Align.right
    }

    private operator fun (Enemy.(time: Float) -> Unit).plus(another: Enemy.(time: Float) -> Unit): Enemy.(time: Float) -> Unit =
        { time ->
            this@plus(time)
            another(time)
        }

    private fun resetPosition(): Enemy.(time: Float) -> Unit = {
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

    private fun sinTrajectory(
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
            val statement = get(index)
            if (statement[0] == 'R') {
                val parts = statement.substring(1).split("-")
                val from = parts.getFloat(0)
                val to = parts.getFloat(1)
                from + Random.nextFloat() * to
            } else {
                statement.toFloat()
            }
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
        val shootingPattern: Int,
        val spawnSide: Int = Align.right, // e.g. "where does it come from?"
        val spawnSideFactor: Float = 0.5f, // e.g. "how far from 0 to SIDE_LENGTH does it come from?"
        val updatePositionDt: Enemy.(time: Float) -> Unit,
        val initialHitPoints: Int,
        val invincibilityPeriod: Float,
        val onDefeat: () -> Char?
    )

    private class Reward(
        val type: Char,
        var condition: Int
    )
}

