package io.itch.mattekudasai.metallance.util.drawing

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self

open class SimpleSprite(internalTexturePath: String): Sprite(Texture(internalTexturePath)), Disposing by Self() {

    init {
        texture.autoDisposing()
    }

}
