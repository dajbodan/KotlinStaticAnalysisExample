package org.example

import org.example.lang.ast.Expr
import org.example.lang.cfg.Node
import org.example.lang.ast.Stmt
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

// Helper Functions to build AST
internal fun block(vararg s: Stmt) = Stmt.Block(s.toList())
internal fun const(v: Any): Expr.Const = Expr.Const(v)
internal fun v(n: String): Expr.Var = Expr.Var(n)
internal fun plus(l: Expr, r: Expr): Expr.Plus = Expr.Plus(l, r)
internal fun minus(l: Expr, r: Expr): Expr.Minus = Expr.Minus(l, r)
internal fun mul(l: Expr, r: Expr): Expr.Mul = Expr.Mul(l, r)
internal fun lt(l: Expr, r: Expr): Expr.Lt = Expr.Lt(l, r)
internal fun eq(l: Expr, r: Expr): Expr.Eq = Expr.Eq(l, r)


// Helper Assertions for Readability
internal fun Node.assertAssignConst(expectedValue: Any) {
    assertTrue(this is Node.Assign, "Node is not assign node but it's $this")
    (this as Node.Assign).value.assertExprConst(expectedValue)
}


internal fun Expr.assertExprConst(expectedValue: Any) {
    assertTrue(this is Expr.Const, "Expr is not folded but it's $this")
    assertEquals(expectedValue, (this as Expr.Const).value)
}

internal fun Node.assertReturnConst(expectedValue: Any) {
    assertTrue(this is Node.Return, "This is not return node, but it's $this")
    (this as Node.Return).result.assertExprConst(expectedValue)
}

internal fun Node.assertCondConst(expectedValue: Any) {
    when (this) {
        is Node.Condition -> this.cond.assertExprConst(expectedValue)
        is Node.While -> this.cond.assertExprConst(expectedValue)
        else -> fail("Node is not neither Condition nor Cycle, it's $this")
    }
}
