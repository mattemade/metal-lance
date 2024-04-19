package io.itch.mattekudasai.metallance.screen.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport

interface EnvironmentRenderer: Disposable {
    fun renderBackground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2?) {}
    fun renderForeground(viewport: Viewport, camera: Camera, time: Float, flagshipPosition: Vector2?) {}
}
