package org.example.ConstantFolding

import org.example.lang.cfg.ControlFlowGraphBuilder
import org.example.lang.ast.Expr
import org.example.lang.cfg.Node
import org.example.lang.ast.Stmt
import kotlin.test.*
import org.example.*
import org.example.analysis.ConstantFolding

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
            Stmt.Assign(v("x"), plus(const(1), const(2))),
                  Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)

        optCfg.assertAssignConst(3)
        val ret = (optCfg as Node.Assign).next
        ret.assertReturnConst(3)
    }

    @Test
    fun `propagates constant variable to return`() {
        // x = 10
        // return x * 2
        val ast = block(
            Stmt.Assign(v("x"), const(10)),
            Stmt.Return(mul(v("x"), const(2)))
        )
        val optCfg = runFold(ast)


        assertTrue(optCfg is Node.Assign)
        val ret = (optCfg as Node.Assign).next
        ret.assertReturnConst(20)
    }

    @Test
    fun `folds complex arithmetic chain`() {
        // x = (1 + 2) * 3  // x = 9
        // y = x + 1        // y = 10
        // return y
        val ast = block(
            Stmt.Assign(v("x"), mul(plus(const(1), const(2)), const(3))),
            Stmt.Assign(v("y"), plus(v("x"), const(1))),
            Stmt.Return(v("y"))
        )
        val optCfg = runFold(ast)


        val assignX = optCfg as Node.Assign
        assignX.assertAssignConst(9)

        val assignY = assignX.next as Node.Assign
        assignY.assertAssignConst(10)

        val ret = assignY.next as Node.Return
        ret.assertReturnConst(10)
    }

    @Test
    fun `merge with different values makes variable Unknown`() {
        // if (true) { x = 1 } else { x = 2 }
        // return x
        val ast = block(
            Stmt.If(
                const(true),
                block(Stmt.Assign(v("x"), const(1))),
                block(Stmt.Assign(v("x"), const(2)))
            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)


        val cond = optCfg as Node.Condition
        val ret = cond.join as Node.Return
        cond.assertCondConst(true)
        assertTrue(ret.result is Expr.Var)
        assertEquals(v("x"), ret.result)
    }

    @Test
    fun `merge with same values propagates constant`() {
        // if (true) { x = 5 } else { x = 5 }
        // return x
        val ast = block(
            Stmt.If(
                const(true),
                block(Stmt.Assign(v("x"), const(5))),
                block(Stmt.Assign(v("x"), const(5)))

            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)
        val cond = optCfg as Node.Condition
        val ret = cond.join as Node.Return

        ret.assertReturnConst(5)
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
            Stmt.Assign(v("y"), const(2)),
            Stmt.If(
                lt(const(10), const(20)),
                block(Stmt.Assign(v("x"), const(1))),
                block(Stmt.Assign(v("x"), const(2)))
            ),
            Stmt.Return(Expr.Minus(Expr.Plus(Expr.Const(5), Expr.Const(6)), Expr.Var("y")))
        )

        val optCfg = runFold(ast)


        assertTrue(optCfg is Node.Assign, "Expected Assign as entry-point")
        val cond = optCfg.next as Node.Condition


        cond.assertCondConst(true)

        val ret = cond.join
        ret.assertReturnConst(9)
    }

    @Test
    fun `variable in cycle becomes Unknown due to back-edge merge`() {
        // x = 0
        // while (x < 3) {
        //   x = x + 1
        // }
        // return x
        val ast = block(
            Stmt.Assign(v("x"), const(0)),
            Stmt.While(
                lt(v("x"), const(3)),
                block(Stmt.Assign(v("x"), plus(v("x"), const(1))))
            ),
            Stmt.Return(v("x"))
        )
        val optCfg = runFold(ast)

        val assign = optCfg as Node.Assign
        assign.assertAssignConst(0) 

        val cycle = assign.next as Node.While

        assertTrue(cycle.cond is Expr.Lt )


        val bodyAssign = cycle.body as Node.Assign
        assertTrue(bodyAssign.value is Expr.Plus)


        val ret = cycle.join as Node.Return
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
            Stmt.Assign(v("x"), const(0)),
            Stmt.While(
                eq( mul(const(2), const(3)), const(6)),
                block(
                    Stmt.Assign(v("x"), plus(v("x"), const(1)))
                )
            ),
            Stmt.Return(v("x"))
        )

        val optCfg = runFold(ast)


        val assign = optCfg as Node.Assign
        assign.assertAssignConst(0)

     
        val cycle = assign.next as Node.While
        cycle.cond.assertExprConst(true)

        val ret = cycle.join as Node.Return
        assertTrue(ret.result is Expr.Var)
        assertEquals(v("x"), ret.result)
    }
}
