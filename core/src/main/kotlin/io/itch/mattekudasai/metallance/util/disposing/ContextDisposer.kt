package io.itch.mattekudasai.metallance.util.disposing

fun interface ContextDisposer {
    fun dispose(context: Any?)
}
