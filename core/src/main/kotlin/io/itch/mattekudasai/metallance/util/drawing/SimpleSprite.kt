package io.itch.mattekudasai.metallance.util.drawing

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import io.itch.mattekudasai.metallance.util.disposing.Disposing
import io.itch.mattekudasai.metallance.util.disposing.Self
import io.itch.mattekudasai.metallance.util.files.overridable

open class SimpleSprite(texture: Texture, private val shouldManagerTextureDisposing: Boolean = false) : Sprite(texture), Disposing by Self() {

    constructor(internalTexturePath: String) : this(Texture(internalTexturePath.overridable), shouldManagerTextureDisposing = true)
    init {
        if (shouldManagerTextureDisposing) {
            texture.autoDisposing()
        }
    }

}
