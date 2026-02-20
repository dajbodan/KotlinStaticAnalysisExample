package org.example.analysis

import org.example.assertAssignConst
import org.example.assertCondConst
import org.example.assertExprConst
import org.example.assertReturnConst
import org.example.block
import org.example.const
import org.example.eq
import org.example.lang.ast.Expr
import org.example.lang.ast.Stmt
import org.example.lang.cfg.ControlFlowGraphBuilder
import org.example.lang.cfg.Node
import org.example.lang.values.Val
import org.example.lt
import org.example.mul
import org.example.plus
import org.example.v
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ConstantFoldingTest {

    internal fun runFold(program: Stmt): Node {
        val cfg = ControlFlowGraphBuilder().build(program)

        val folder = ConstantFolding(cfg)
        folder.run()
        return folder.buildNewAST()
    }


    @Test
    fun `folds simple constant expression in assign`() {
        // x = 1 + 2
        // return x
        val ast = block(
            Stmt.Assign(v("x"), plus(const(Val.IntV(1)), const(Val.IntV(2)))),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)

        optCfg.assertAssignConst(Val.IntV(3))
        val ret = (optCfg as Node.Assign).next
        ret.assertReturnConst(Val.IntV(3))
    }

    @Test
    fun `propagates constant variable to return`() {
        // x = 10
        // return x * 2
        val ast = block(
            Stmt.Assign(v("x"), const(Val.IntV(10))),
            Stmt.Return(mul(v("x"), const(Val.IntV(2))))
        )
        val optCfg = runFold(ast)


        assertTrue(optCfg is Node.Assign)
        val ret = (optCfg as Node.Assign).next
        ret.assertReturnConst(Val.IntV(20))
    }

    @Test
    fun `folds complex arithmetic chain`() {
        // x = (1 + 2) * 3  // x = 9
        // y = x + 1        // y = 10
        // return y
        val ast = block(
            Stmt.Assign(v("x"), mul(plus(const(Val.IntV(1)), const(Val.IntV(2))), const(Val.IntV(3)))),
            Stmt.Assign(v("y"), plus(v("x"), const(Val.IntV(1)))),
            Stmt.Return(v("y"))
        )
        val optCfg = runFold(ast)


        val assignX = optCfg as Node.Assign
        assignX.assertAssignConst(Val.IntV(9))

        val assignY = assignX.next as Node.Assign
        assignY.assertAssignConst(Val.IntV(10))

        val ret = assignY.next as Node.Return
        ret.assertReturnConst(Val.IntV(10))
    }

    @Test
    fun `merge with different values makes variable Unknown`() {
        // if (true) { x = 1 } else { x = 2 }
        // return x
        val ast = block(
            Stmt.If(
                const(Val.BoolV(true)),
                block(Stmt.Assign(v("x"), const(Val.IntV(1)))),
                block(Stmt.Assign(v("x"), const(Val.IntV(2))))
            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)


        val cond = optCfg as Node.Condition
        val ret = cond.MergeBlock as Node.Return
        cond.assertCondConst(Val.BoolV(true))
        assertTrue(ret.result is Expr.Var)
        assertEquals(v("x"), ret.result)
    }

    @Test
    fun `merge with same values propagates constant`() {
        // if (true) { x = 5 } else { x = 5 }
        // return x
        val ast = block(
            Stmt.If(
                const(Val.BoolV(true)),
                block(Stmt.Assign(v("x"), const(Val.IntV(5)))),
                block(Stmt.Assign(v("x"), const(Val.IntV(5))))

            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)
        val cond = optCfg as Node.Condition
        val ret = cond.MergeBlock as Node.Return

        ret.assertReturnConst(Val.IntV(5))
    }

    @Test
    fun `folds constant expression in If condition and propagates state through merge`() {
        // y = 2
        // if (10 < 20)
        //        x = 1
        // else
        //        x = 2
        // return 5 + 6 - y
        //
        val ast = block(
            Stmt.Assign(v("y"), const(Val.IntV(2))),
            Stmt.If(
                lt(const(Val.IntV(10)), const(Val.IntV(20))),
                block(Stmt.Assign(v("x"), const(Val.IntV(1)))),
                block(Stmt.Assign(v("x"), const(Val.IntV(2))))
            ),
            Stmt.Return(Expr.Minus(Expr.Plus(Expr.Const(Val.IntV(5)), Expr.Const(Val.IntV(6))), Expr.Var("y")))
        )

        val optCfg = runFold(ast)


        assertTrue(optCfg is Node.Assign, "Expected Assign as entry-point")
        val cond = optCfg.next as Node.Condition


        cond.assertCondConst(Val.BoolV(true))

        val ret = cond.MergeBlock
        ret.assertReturnConst(Val.IntV(9))
    }

    @Test
    fun `variable in cycle becomes Unknown due to back-edge merge`() {
        // x = 0
        // while (x < 3) {
        //   x = x + 1
        // }
        // return x
        val ast = block(
            Stmt.Assign(v("x"), const(Val.IntV(0))),
            Stmt.While(
                lt(v("x"), const(Val.IntV(3))),
                block(Stmt.Assign(v("x"), plus(v("x"), const(Val.IntV(1)))))
            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)

        val assign = optCfg as Node.Assign
        assign.assertAssignConst(Val.IntV(0))

        val cycle = assign.next as Node.While

        assertTrue(cycle.cond is Expr.Lt)


        val bodyAssign = cycle.body as Node.Assign
        assertTrue(bodyAssign.value is Expr.Plus)


        val ret = cycle.next as Node.Return
        assertTrue(ret.result is Expr.Var)
        assertEquals(v("x"), ret.result)
    }

    @Test
    fun `folds constant expression in cycle condition and makes var Unknown after loop`() {
        // x = 0
        // while (2 * 3 == 6) {
        //   x = x + 1
        // }
        // return x
        val ast = block(
            Stmt.Assign(v("x"), const(Val.IntV(0))),
            Stmt.While(
                eq(mul(const(Val.IntV(2)), const(Val.IntV(3))), const(Val.IntV(6))),
                block(
                    Stmt.Assign(v("x"), plus(v("x"), const(Val.IntV(1))))
                )
            ),
            Stmt.Return(v("x"))
        )

        val optCfg = runFold(ast)


        val assign = optCfg as Node.Assign
        assign.assertAssignConst(Val.IntV(0))


        val cycle = assign.next as Node.While
        cycle.cond.assertExprConst(Val.BoolV(true))

        val ret = cycle.next as Node.Return
        assertTrue(ret.result is Expr.Var)
        assertEquals(v("x"), ret.result)
    }
}