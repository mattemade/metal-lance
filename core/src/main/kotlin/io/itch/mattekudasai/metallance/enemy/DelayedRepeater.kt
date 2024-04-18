package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val nextDelay: (counter: Int, time: Float) -> Float,
    initialDelay: Float = nextDelay(0, 0f),
    private val action: (counter: Int, periodicTime: Float, total: Float) -> Boolean,
) {
    private var periodicTimer = 0f
    private var internalTimer = 0f
    private var repeatedCounter = 0
    private var currentDelay = initialDelay

    fun update(delta: Float): Boolean {
        periodicTimer += delta
        internalTimer += delta
        var result = true
        while (result && periodicTimer >= currentDelay) {
            repeatedCounter++
            result = action(repeatedCounter, periodicTimer, internalTimer)
            periodicTimer -= currentDelay
            currentDelay = nextDelay(repeatedCounter, periodicTimer)
        }
        return result
    }

}
