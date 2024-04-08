package io.itch.mattekudasai.metallance.enemy

class DelayedRepeater(
    private val initialDelay: Float = -1f,
    private val repeatPeriod: Float = -1f,
    private val action: () -> Unit
) {
    private var internalTimer = 0f
    private var initialDelayPassed = initialDelay < 0f

    fun update(delta: Float) {
        internalTimer += delta
        if (!initialDelayPassed && initialDelay >= 0f && internalTimer >= initialDelay) {
            action()
            initialDelayPassed = true
            internalTimer -= initialDelay
        }
        if (initialDelayPassed) {
            while (internalTimer >= repeatPeriod) {
                action()
                internalTimer -= repeatPeriod
            }
        }
    }

}
