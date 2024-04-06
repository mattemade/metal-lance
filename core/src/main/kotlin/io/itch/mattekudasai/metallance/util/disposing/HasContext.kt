package io.itch.mattekudasai.metallance.util.disposing

interface HasContext<DisposingContext> {
    val context: Map<Class<DisposingContext>, DisposingContext>
}
