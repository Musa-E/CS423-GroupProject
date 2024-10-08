package com.myscript.iink.demo.data

import com.myscript.iink.demo.ui.PartState

// AI STATEMENT: this file used zero AI and was made by our team 100%


/*
This class is, basically, a global variable to store all the PartStates in for the listview. This allows the
list to stay alive, even if you call TaskListView.java 100 times, instead of the list refreshing, which would stink.
 */
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

    //Checks to see if that part exists in the list
    @Synchronized
    fun checkForPart(partState: PartState): Boolean {
        for(x in partStateArrayList) {
            if(x.partId == partState.partId){
                return true
            }
        }
        return false
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
