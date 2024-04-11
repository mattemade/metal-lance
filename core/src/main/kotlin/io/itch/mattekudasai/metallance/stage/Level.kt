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

    val lines = scriptFile.reader().readLines()
    val repeaters = mutableListOf<DelayedRepeater>()
    var currentIndex = 0
    var waitTime = 0f

    fun update(delta: Float) {
        waitTime -= delta
        while (waitTime < 0f && currentIndex < lines.size) {
            val line = lines[currentIndex++]
            val split = line.split("  ")
            when (line[0]) {
                '#' -> {}
                'N' -> winningCondition(split.getString(1), split.getInt(2))
                'B' -> setBackground(split.getString(1))
                'D' -> setRenderMode(split.getInt(1), split.getInt(2))
                'M' -> playMusic(split.getString(1), split.getFloat(2))
                'C' -> setTint(Color(split.getFloat(1), split.getFloat(2), split.getFloat(3), 1f))
                'T' -> showText(
                    TextConfiguration(
                        text = split.getString(1).split("\\"),
                        positionX = split.getFloat(2),
                        positionY = split.getFloat(3)
                    )
                )

                'W' -> waitTime += split.getFloat(1)
                'R' -> repeaters += DelayedRepeater(
                    nextDelay = { split.getFloat(1) },
                    action = {
                        val enemyLine = split.getString(2)
                        val trajectory = split.getString(3)
                        spawnEnemy(
                            EnemyConfiguration(
                                enemyType = enemyLine[0] - 'A',
                                shootingPattern = enemyLine.substring(1).toInt(),
                                alignment = when (trajectory) {
                                    "RTL" -> Align.right
                                    else -> Align.right
                                },
                                alignmentFactor = split.getFloat(4),
                                updatePositionDt = { time ->
                                    when (trajectory) {
                                        "RTL" -> internalPosition.set(initialPosition)
                                            .sub(50f * time, sin(time * split.getFloat(5)) * split.getFloat(6))
                                    }
                                }
                            )
                        )
                    },
                    initialDelay = 0f,
                )
                'E' -> endSequence()
            }
        }

        repeaters.forEach { it.update(delta) }
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

    class TextConfiguration(
        val text: List<String>,
        val positionX: Float,
        val positionY: Float,
    )

    class EnemyConfiguration(
        val enemyType: Int,
        val shootingPattern: Int,
        val alignment: Int = Align.right, // e.g. "where does it come from?"
        val alignmentFactor: Float = 0.5f, // e.g. "how far from 0 to SIDE_LENGTH does it come from?"
        val updatePositionDt: Enemy.(time: Float) -> Unit
    )
}
