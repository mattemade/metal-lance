package io.itch.mattekudasai.metallance.util.drawing

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.files.overridable

class MonoSpaceTextDrawer(
    fontFileName: String,
    private val alphabet: String,
    private val fontLetterWidth: Int,
    private val fontLetterHeight: Int,
    private val fontHorizontalSpacing: Int = 0,
    private val fontVerticalSpacing: Int = 0,
    private val fontHorizontalPadding: Int = 0,
    private val fontVerticalPadding: Int = 0,
    val drawingLetterWidth: Float = fontLetterWidth.toFloat(),
    val drawingLetterHeight: Float = fontLetterHeight.toFloat(),
    val drawingHorizontalSpacing: Float = 1f,
    val drawingVerticalSpacing: Float = 0f,
) : Disposing by Self() {

    private val font: Texture by remember { Texture(fontFileName.overridable) }
    private val letters = mutableMapOf<Char, TextureRegion>().apply {
        var column = 0
        var row = 0
        val letterWidthWithSpacing = fontLetterWidth + fontHorizontalSpacing
        val letterHeightWithSpacing = fontLetterHeight + fontVerticalSpacing
        var yPosition = fontVerticalPadding
        alphabet.forEach { key ->
            val xPosition = ((column++) * letterWidthWithSpacing + fontHorizontalPadding).let {
                if (it > font.width) {
                    column = 0
                    yPosition = (++row) * letterHeightWithSpacing + fontVerticalPadding
                    0
                } else {
                    it
                }
            }

            put(
                key,
                TextureRegion(
                    font,
                    xPosition,
                    yPosition,
                    fontLetterWidth,
                    fontLetterHeight
                )
            )
        }

    }

    private val Int.drawingWidth: Float
        get() = this * fontLetterWidth + (this - 1) * drawingHorizontalSpacing

    private val String.drawingWidth: Float
        get() = length.drawingWidth

    /** @return number of drawn characters: caller could use it to check if it is changed  */
    fun drawText(
        batch: SpriteBatch,
        text: List<String>,
        x: Float,
        y: Float,
        align: Int = Align.topRight,
        characterLimit: Int = Int.MAX_VALUE,
    ): Int {
        if (text.size == 0) {
            return 0
        }
        val textBoxWidth = text.maxOf { it.length }.drawingWidth
        val textBoxHeight = text.size.let { it * drawingLetterHeight + (it-1) * drawingVerticalSpacing }
        val startPositionX: Float = when {
            Align.isRight(align) -> x
            Align.isLeft(align) -> x - textBoxWidth
            Align.isCenterHorizontal(align) -> x - textBoxWidth/2f
            else -> throw IllegalArgumentException("Provide a correct align instead of $align")
        }
        val startPositionY: Float = when {
            Align.isTop(align) -> y
            Align.isBottom(align) -> y - textBoxHeight
            Align.isCenterVertical(align) -> y - textBoxHeight/2f
            else -> throw IllegalArgumentException("Provide a correct align instead of $align")
        }
        var drawnCharacters = 0
        text.forEachIndexed { row, line ->
            val lineStartX = startPositionX + (textBoxWidth - line.length.drawingWidth) / 2f
            line.forEachIndexed { column, char ->
                if (char != ' ' && drawnCharacters < characterLimit) {
                    drawnCharacters++
                    batch.draw(
                        letters[char],
                        lineStartX + column * (drawingLetterWidth + drawingHorizontalSpacing),
                        startPositionY + textBoxHeight - row * (drawingLetterHeight + drawingVerticalSpacing),
                        drawingLetterWidth,
                        drawingLetterHeight
                    )
                }
            }
        }
        return drawnCharacters
    }
}
