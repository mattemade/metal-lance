package io.itch.mattekudasai.metallance.util.disposing

import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely

class MutableDisposingList<T: Disposable>(private val list: MutableList<T> = mutableListOf()): MutableList<T> by list, Disposable {

    override fun clear() {
        list.forEach { it.disposeSafely() }
        list.clear()
    }

    override fun removeAt(index: Int): T =
        list.removeAt(index).also { it.disposeSafely() }

    override fun set(index: Int, element: T): T =
        list.set(index, element).also { it.disposeSafely() }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(element: T): Boolean {
        return list.remove(element)
    }

    override fun dispose() {
        clear()
    }
}
inline fun <T: Disposable> mutableDisposableListOf(): MutableDisposingList<T> = MutableDisposingList()
