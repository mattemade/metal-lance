package io.itch.mattekudasai.metallance.screen.touch

import com.badlogic.gdx.math.Vector2
import io.itch.mattekudasai.metallance.GlobalState
import ktx.app.KtxInputAdapter
import kotlin.math.abs

class TouchMenuAdapter(
    private val onDragDown: () -> Unit,
    private val onDragUp: () -> Unit,
    private val onTap: () -> Unit,
) : KtxInputAdapter {

    private val lastTouchDown = Vector2()
    private var lastTouchTime: Long = 0

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == 0) {
            lastTouchDown.set(screenX.toFloat(), screenY.toFloat())
            lastTouchTime = System.currentTimeMillis()
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer == 0) {
            val deltaY = screenY.toFloat() - lastTouchDown.y
            if (abs(deltaY) > 400f / GlobalState.scaleFactor) {
                if (deltaY > 0) {
                    onDragDown()
                } else {
                    onDragUp()
                }
                lastTouchDown.y = screenY.toFloat()
            }
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == 0) {
            lastTouchDown.sub(screenX.toFloat(), screenY.toFloat())
            lastTouchTime = System.currentTimeMillis() - lastTouchTime
            if (lastTouchDown.len() < 100f / GlobalState.scaleFactor) {
                onTap()
            }
        }
        return super.touchUp(screenX, screenY, pointer, button)
    }
}
