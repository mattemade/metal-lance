package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val nextDelay: () -> Float,
    private val action: () -> Unit
) {
    private var internalTimer = 0f
    private var currentDelay = nextDelay()

    fun update(delta: Float) {
        internalTimer += delta
        while (internalTimer >= currentDelay) {
            action()
            internalTimer -= currentDelay
            currentDelay = nextDelay()
        }
    }

}
