package io.itch.mattekudasai.metallance.player

import com.badlogic.gdx.graphics.Texture
import io.itch.mattekudasai.metallance.util.drawing.SimpleSprite

class Shot(x: Float, y: Float, texture: Texture): SimpleSprite(texture) {

    private var internalX = x
    private var internalY = y
    fun update(delta: Float): Float {
        internalX += delta * SPEED
        setPosition((internalX - width / 2f).toInt().toFloat(), (internalY - height / 2f).toInt().toFloat())
        return internalX
    }

    companion object {
        const val SPEED = 300f
    }

}
