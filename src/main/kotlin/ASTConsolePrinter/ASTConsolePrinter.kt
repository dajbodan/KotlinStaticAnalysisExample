package org.example.ASTConsolePrinter

import org.example.Expression.Expr
import org.example.Expression.Visitor.ExprVisitor
import org.example.Statement.Stmt
import org.example.Statement.Visitor.StmtVisitor





internal class ASTConsolePrinter(
    private val out: Appendable = System.out
) : ExprVisitor<Unit>, StmtVisitor<Unit> {

    private var indentLevel = 0
    private val indentChar = "\t" 

  
    private fun indent() {
        out.append(indentChar.repeat(indentLevel))
    }

    
    private inline fun indented(block: () -> Unit) {
        indentLevel++
        block()
        indentLevel--
    }



    override fun visitConst(x: Expr.Const) {
        out.append("Const(${x.value})")
    }
    override fun visitVar(x: Expr.Var) {
        out.append("Var(${x.name})")
    }

    override fun visitEq(x: Expr.Eq) {
        out.append("Eq(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }

    override fun visitNeq(x: Expr.Neq) {
        out.append("Neq(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }

    override fun visitLt(x: Expr.Lt) {
        out.append("Lt(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }

    override fun visitPlus(x: Expr.Plus) {
        out.append("Plus(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }

    override fun visitMinus(x: Expr.Minus) {
        out.append("Minus(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }

    override fun visitMul(x: Expr.Mul) {
        out.append("Mul(")
        x.left.accept(this)
        out.append(", ")
        x.right.accept(this)
        out.append(")")
    }


    override fun visitIf(x: Stmt.If) {
        indent()
        out.append("If\n")
        indented {

            indent()
            x.cond.accept(this)
            out.append("\n")


            indent()
            out.append("Then\n")
            indented { x.thenBranch.accept(this) }


            x.elseBranch?.let {
                indent()
                out.append("Else\n")
                indented { it.accept(this) }
            }
        }
    }

    override fun visitBlock(x: Stmt.Block) {
        indent()
        out.append("Block\n")
        indented {
            x.stmts.forEach { it.accept(this) }
        }
    }

    override fun visitReturn(x: Stmt.Return) {
        indent()
        out.append("Return\n")
        indented {
            indent()
            x.result.accept(this)
            out.append("\n")
        }
    }

    override fun visitAssign(x: Stmt.Assign) {
        indent()
        out.append("Assign ${x.variable.name}\n")
        indented {
            indent()
            x.value.accept(this)
            out.append("\n")
        }
    }

    override fun visitCycle(x: Stmt.While) {
        indent()
        out.append("While\n")
        indented {
            indent()
            x.cond.accept(this)
            out.append("\n")

            indent()
            out.append("Body\n")
            indented { x.body.accept(this) }
        }
    }
}
