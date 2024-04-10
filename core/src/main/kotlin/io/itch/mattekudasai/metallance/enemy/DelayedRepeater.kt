package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val nextDelay: () -> Float,
    initialDelay: Float = nextDelay(),
    private val action: () -> Unit,
) {
    private var internalTimer = 0f
    private var currentDelay = initialDelay

    fun update(delta: Float) {
        internalTimer += delta
        while (internalTimer >= currentDelay) {
            action()
            internalTimer -= currentDelay
            currentDelay = nextDelay()
        }
    }

}
