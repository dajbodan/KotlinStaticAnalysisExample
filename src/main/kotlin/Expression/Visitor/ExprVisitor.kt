package org.example.Expression.Visitor

import org.example.Expression.Expr

interface ExprVisitor<out T>
{
    fun visitConst(x : Expr.Const) : T
    fun visitVar(x : Expr.Var) : T

    fun visitEq(x : Expr.Eq) : T
    fun visitNeq(x : Expr.Neq) : T
    fun visitLt(x : Expr.Lt) : T

    fun visitPlus(x : Expr.Plus) : T
    fun visitMinus(x : Expr.Minus) : T
    fun visitMul(x : Expr.Mul) : T
}