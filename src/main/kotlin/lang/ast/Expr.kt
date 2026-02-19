package org.example.lang.ast

import org.example.lang.ast.visitor.ExprVisitor

sealed interface Expr  {
    fun <V> accept(x: ExprVisitor<V>): V




    data class Const(val value: Any) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>)  = x.visitConst(this)

    }
    data class Var(val name: String) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitVar(this)
    }


    data class Eq(val left: Expr, val right: Expr) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitEq(this)

    }
    data class Neq(val left: Expr, val right: Expr) : Expr
    {
        override fun <V>accept(x: ExprVisitor<V>) = x.visitNeq(this)


    }
    data class Lt(val left: Expr, val right: Expr) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitLt(this)

    }


    data class Plus(val left: Expr, val right: Expr) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitPlus(this)

    }
    data class Minus(val left: Expr, val right: Expr) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitMinus(this)

    }
    data class Mul(val left: Expr, val right: Expr) : Expr
    {
        override fun <V> accept(x: ExprVisitor<V>) = x.visitMul(this)

    }

}
