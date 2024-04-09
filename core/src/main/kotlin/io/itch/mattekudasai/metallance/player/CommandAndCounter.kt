package io.itch.mattekudasai.metallance.player

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CommandAndCounter: ReadWriteProperty<State, Boolean> {
    private var counter: Int = 0
    override fun getValue(thisRef: State, property: KProperty<*>): Boolean =
        counter > 0

    override fun setValue(thisRef: State, property: KProperty<*>, value: Boolean) {
        if (value) {
            counter++
        } else if (counter > 0) {
            counter--
        }
    }

    companion object {
        fun commandAndCounter() = CommandAndCounter()
    }
}
