package org.example.lang.cfg

interface NodeVisitor<out T> {
    fun visitAssign(x : Node.Assign) : T
    fun visitReturn(x : Node.Return) : T
    fun visitConditional(x : Node.Condition) : T
    fun visitQuit(x : Node.Quit) : T
    fun visitCycle(X : Node.While) : T
}