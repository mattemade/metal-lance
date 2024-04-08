package io.itch.mattekudasai.metallance.util.disposing

import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely

internal class Self : Disposing {
    private val disposables = mutableSetOf<Disposable>()
    private val contextDisposers = mutableMapOf<Class<*>, (Any?) -> Unit>()

    override fun <T : Disposable> remember(block: () -> T): Lazy<T> =
        lazy { managed(block) }

    override fun <T : Disposable> T.autoDisposing(): T =
        this.also { disposables += it }

    override fun <T : Disposable> managed(block: () -> T): T =
        block().autoDisposing()

    override fun <K> K.registerAsContextDisposer(applicableTo: Class<*>, block: K.(Any?) -> Unit): K {
        contextDisposers[applicableTo] = { this.block(it) }
        return this
    }

    override fun <T : Disposable> forget(disposable: T) {
        disposables.remove(disposable)
    }

    override fun dispose() {
        disposables.forEach {
            if (it is HasContext<*>) {
                it.context.forEach { (clazz, context) ->
                    contextDisposers[clazz]?.invoke(context)
                }
            }

            it.disposeSafely()
        }
        disposables.clear()
    }
}
