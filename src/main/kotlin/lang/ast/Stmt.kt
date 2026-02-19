package org.example.lang.ast

import org.example.lang.ast.visitor.StmtVisitor

sealed interface Stmt {
    fun <T> accept(x: StmtVisitor<T>): T
    data class Block(val stmts: List<Stmt>) : Stmt {
        override fun <T>  accept(x: StmtVisitor<T>) : T = x.visitBlock(this)
    }
    data class Assign(val variable: Expr.Var, val value: Expr) : Stmt
    {
        override fun <T> accept(x: StmtVisitor<T>) = x.visitAssign(this)
    }
    data class While(
        val cond: Expr,
        val body: Stmt
    ) : Stmt
    {
        override fun <T> accept(x: StmtVisitor<T>): T = x.visitCycle(this)
    }

    data class If(
        val cond: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?
    ) : Stmt
    {
        override fun <T> accept(x: StmtVisitor<T>) = x.visitIf(this)
    }

    data class Return(val result: Expr) : Stmt
    {
        override fun <T> accept(x: StmtVisitor<T>) = x.visitReturn(this)
    }
}