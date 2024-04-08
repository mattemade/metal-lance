@file:JvmName("TeaVMLauncher")

package io.itch.mattekudasai.metallance.teavm

import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import io.itch.mattekudasai.metallance.MetalLanceGame

/** Launches the TeaVM/HTML application. */
fun main() {
    val config = TeaApplicationConfiguration("canvas").apply {
        width = 0 // use all available space
        height = 0 // use all available space
    }
    TeaApplication(MetalLanceGame(), config)
}
