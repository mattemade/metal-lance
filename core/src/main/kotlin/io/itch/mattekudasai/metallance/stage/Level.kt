package io.itch.mattekudasai.metallance.stage

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Align
import io.itch.mattekudasai.metallance.enemy.DelayedRepeater
import io.itch.mattekudasai.metallance.enemy.Enemy
import kotlin.math.sin

class Level(
    scriptFile: FileHandle,
    private val showText: (TextConfiguration) -> Unit,
    private val spawnEnemy: (EnemyConfiguration) -> Unit,
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
                'T' -> showText(
                    TextConfiguration(
                        text = split[1].split("\\"),
                        positionX = split[2].toFloat(),
                        positionY = split[3].toFloat()
                    )
                )

                'W' -> waitTime += split[1].toFloat()
                'R' -> repeaters += DelayedRepeater(
                    nextDelay = { split[1].toFloat() },
                    action = {
                        val enemyLine = split[2]
                        val trajectory = split[3]
                        spawnEnemy(
                            EnemyConfiguration(
                                enemyType = enemyLine[0] - 'A',
                                shootingPattern = enemyLine.substring(1).toInt(),
                                alignment = when (trajectory) {
                                    "RTL" -> Align.right
                                    else -> Align.right
                                },
                                alignmentFactor = split[4].toFloat(),
                                updatePositionDt = { time ->
                                    when (trajectory) {
                                        "RTL" -> internalPosition.set(initialPosition)
                                            .sub(50f * time, sin(time * split[5].toFloat()) * split[6].toFloat())
                                    }
                                }
                            )
                        )
                    },
                    initialDelay = 0f,
                )
            }
        }

        repeaters.forEach { it.update(delta) }
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
