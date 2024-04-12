package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val nextDelay: (counter: Int, time: Float) -> Float,
    initialDelay: Float = nextDelay(0, 0f),
    private val action: (counter: Int, time: Float) -> Boolean,
) {
    private var internalTimer = 0f
    private var repeatedCounter = 0
    private var currentDelay = initialDelay

    fun update(delta: Float): Boolean {
        internalTimer += delta
        var result = true
        while (result && internalTimer >= currentDelay) {
            repeatedCounter++
            result = action(repeatedCounter, internalTimer)
            internalTimer -= currentDelay
            currentDelay = nextDelay(repeatedCounter, internalTimer)
        }
        return result
    }

}
