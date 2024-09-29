package com.myscript.iink.demo.data

import com.myscript.iink.demo.ui.PartState

// AI STATEMENT: this file used zero AI and was made by our team 100%

class ArrayRepository private constructor() {
    private val partStateArrayList: ArrayList<PartState> = ArrayList()

    @Synchronized
    fun addPartState(partState: PartState) {
        partStateArrayList.add(partState)
    }

    @Synchronized
    fun removePartState(partState: PartState) {
        partStateArrayList.remove(partState)
    }

    @get:Synchronized
    val partStates: List<PartState>
        get() = ArrayList(partStateArrayList) // Return a copy

    val isEmpty: Boolean
        get() = partStateArrayList.isEmpty()

    companion object {
        @Volatile
        private var instance: ArrayRepository? = null

        @JvmStatic
        fun getInstance(): ArrayRepository {
            return instance ?: synchronized(this) {
                instance ?: ArrayRepository().also { instance = it }
            }
        }
    }
}
