package io.itch.mattekudasai.metallance.util.disposing

import com.badlogic.gdx.utils.Disposable

interface Disposing : Disposable {
    fun <T : Disposable> remember(block: () -> T): Lazy<T>
    fun <T : Disposable> T.autoDisposing(): T
    fun <T : Disposable> managed(block: () -> T): T
    fun <K> K.registerAsContextDisposer(applicableTo: Class<*>, block: K.(Any?) -> Unit): K
    fun <T: Disposable> forget(disposable: T)
}
