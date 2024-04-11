package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val nextDelay: (time: Float) -> Float,
    initialDelay: Float = nextDelay(0f),
    private val action: () -> Unit,
) {
    private var internalTimer = 0f
    private var currentDelay = initialDelay

    fun update(delta: Float) {
        internalTimer += delta
        while (internalTimer >= currentDelay) {
            action()
            internalTimer -= currentDelay
            currentDelay = nextDelay(internalTimer)
        }
    }

}
