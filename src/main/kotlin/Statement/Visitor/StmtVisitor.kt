package org.example.Statement.Visitor

import org.example.Statement.Stmt

interface StmtVisitor<out T>
{
    fun visitIf(x : Stmt.If) : T
    fun visitBlock(x : Stmt.Block) : T
    fun visitReturn(x : Stmt.Return) : T
    fun visitAssign(x : Stmt.Assign) : T
    fun visitCycle(x : Stmt.While) : T
}