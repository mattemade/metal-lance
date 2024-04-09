@file:JvmName("Lwjgl3Launcher")

package io.itch.mattekudasai.metallance.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.itch.mattekudasai.metallance.MetalLanceGame

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return
    Lwjgl3Application(MetalLanceGame(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("metal-lance")
        setWindowedMode(1024, 800)
        useVsync(false)

        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
