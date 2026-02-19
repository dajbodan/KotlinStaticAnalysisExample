package org.example.BuilderControlFlowGraphTest



import org.example.lang.cfg.ControlFlowGraphBuilder
import org.example.lang.ast.Expr
import org.example.lang.cfg.Node
import org.example.lang.ast.Stmt
import kotlin.test.*

class ControlFlowGraphBuilderTest {

    private fun block(vararg s: Stmt) = Stmt.Block(s.toList())

    @Test
    fun `linear block wires Assign to Return and Return is terminal`() {
        val ast = block(
            Stmt.Assign(Expr.Var("x"), Expr.Const(1)),
            Stmt.Return(Expr.Var("x"))
        )

        val entry = ControlFlowGraphBuilder().build(ast)
        assertTrue(entry is Node.Assign)
        val assign = entry as Node.Assign
        assertTrue(assign.next is Node.Return)

        val ret = assign.next as Node.Return

        when (ret) {
            is Node.Return -> { }
            else -> fail("Return node expected")
        }
    }

    @Test
    fun `if without else falls through to afterIf`() {
        val ast = block(
            Stmt.Assign(Expr.Var("x"), Expr.Const(0)),
            Stmt.If(
                cond = Expr.Lt(Expr.Var("x"), Expr.Const(1)),
                thenBranch = block(Stmt.Assign(Expr.Var("y"), Expr.Const(10))),
                elseBranch = null
            ),
            Stmt.Return(Expr.Var("y"))
        )

        val entry = ControlFlowGraphBuilder().build(ast)

        val assignX = entry as Node.Assign
        val cond = assignX.next as Node.Condition


        val thenHead = cond.nextIfTrue
        val elseHead = cond.nextIfFalse
        val afterIf = cond.join


        assertTrue(thenHead is Node.Assign)

        assertTrue(thenHead.variable == Expr.Var("y") )
        assertSame(afterIf, (thenHead as Node.Assign).next)


        assertSame(afterIf, elseHead)


        assertTrue(afterIf is Node.Return)
    }

    @Test
    fun `if with else wires both branches to the same afterIf`() {
        val ast = block(
            Stmt.Assign(Expr.Var("x"), Expr.Const(0)),
            Stmt.If(
                cond = Expr.Eq(Expr.Var("x"), Expr.Const(0)),
                thenBranch = block(Stmt.Assign(Expr.Var("y"), Expr.Const(1))),
                elseBranch = block(Stmt.Assign(Expr.Var("y"), Expr.Const(2)))
            ),
            Stmt.Return(Expr.Var("y"))
        )

        val entry = ControlFlowGraphBuilder().build(ast)
        val cond = (entry as Node.Assign).next as Node.Condition

        val afterIf = cond.join
        assertTrue(afterIf is Node.Return)

        val thenAssign = cond.nextIfTrue as Node.Assign
        val elseAssign = cond.nextIfFalse as Node.Assign
        assertEquals(Expr.Var("y"), thenAssign.variable)
        assertEquals(Expr.Var("y"), elseAssign.variable)
        assertSame(afterIf, thenAssign.next)
        assertSame(afterIf, elseAssign.next)
    }

    @Test
    fun `cycle creates back edge from body tail to condition and NO-edge to after`() {
        val ast = block(
            Stmt.Assign(Expr.Var("i"), Expr.Const(0)),
            Stmt.While(
                cond = Expr.Lt(Expr.Var("i"), Expr.Const(3)),
                body = block(Stmt.Assign(Expr.Var("i"), Expr.Plus(Expr.Var("i"), Expr.Const(1))))
            ),
            Stmt.Return(Expr.Var("i"))
        )

        val entry = ControlFlowGraphBuilder().build(ast)
        val assignI0 = entry as Node.Assign
        val cycle = assignI0.next as Node.While


        val bodyHead = cycle.body
        assertTrue(bodyHead is Node.Assign)
        assertEquals(Expr.Var("i"), bodyHead.variable)


        val bodyAssign = bodyHead as Node.Assign
        assertSame(cycle, bodyAssign.next)


        val after = cycle.join
        assertTrue(after is Node.Return)
    }

    @Test
    fun `block visiting is reversed (later stmt becomes next of former)`() {
        val ast = block(
            Stmt.Assign(Expr.Var("a"), Expr.Const(1)),
            Stmt.Assign(Expr.Var("b"), Expr.Const(2)),
            Stmt.Return(Expr.Var("b"))
        )

        val entry = ControlFlowGraphBuilder().build(ast)
        val a = entry as Node.Assign
        val b = a.next as Node.Assign
        val ret = b.next as Node.Return

        assertEquals("a", a.variable.name)
        assertEquals("b", b.variable.name)
        assertTrue(ret is Node.Return)
    }

    @Test
    fun `return in the middle makes subsequent statements unreachable (not linked)`() {
        val ast = block(
            Stmt.Return(Expr.Const(0)),
            Stmt.Assign(Expr.Var("z"), Expr.Const(1)) // should not be linked from entry
        )

        val entry = ControlFlowGraphBuilder().build(ast)
        assertTrue(entry is Node.Return)

    }
}