package org.example.lang.ast.print

import org.example.lang.ast.Expr
import org.example.lang.ast.visitor.ExprVisitor

class InfixExpresionPrinter : ExprVisitor<String>
{
    fun print(x : Expr) : String
    {
        return x.accept(this)
    }
    override fun visitConst(x: Expr.Const) : String = x.value.toString()

    override fun visitVar(x: Expr.Var) : String = x.name

    override fun visitEq(x: Expr.Eq) : String = x.left.accept(this) + "==" + x.right.accept(this)

    override fun visitNeq(x: Expr.Neq) : String = x.left.accept(this) + "!=" + x.right.accept(this)

    override fun visitLt(x: Expr.Lt) : String = x.left.accept(this) + "<" + x.right.accept(this)

    override fun visitPlus(x: Expr.Plus) : String = "(" + x.left.accept(this) + "+" +   x.right.accept(this) + ")"

    override fun visitMinus(x: Expr.Minus) : String = "("+ x.left.accept(this) + "-" + x.right.accept(this) + ")"

    override fun visitMul(x: Expr.Mul) : String = x.left.accept(this) + "*" + x.right.accept(this)

}