package org.example.lang.values

sealed interface Val {
    data class IntV(val v:Int): Val
    {
        override fun toString(): String = v.toString()
    }
    data class BoolV(val v:Boolean): Val
    {
        override fun toString() = v.toString()
    }
}
