package io.itch.mattekudasai.metallance.util.files

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

val String.overridable: FileHandle
    get() = Gdx.files.local("assets/$this").takeIf { it.exists() } ?: Gdx.files.internal(this)
