package com.myscript.iink.demo.data

class currentStringStorage {
    private var blockString: String = null.toString()

    fun getBlockString(): String {
        return blockString
    }

    fun setBlockString(string: String){
        this.blockString = blockString
    }
}