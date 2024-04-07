package io.itch.mattekudasai.metallance.util.drawing

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self

open class SimpleSprite(texture: Texture) : Sprite(texture), Disposing by Self() {

    constructor(internalTexturePath: String) : this(Texture(internalTexturePath))

    init {
        texture.autoDisposing()
    }

}
