package org.example.BuilderControlFlowGraph

import org.example.Node.*
import org.example.Statement.Stmt
import org.example.Statement.Visitor.StmtVisitor

internal class ControlFlowGraphBuilder:  StmtVisitor<Node>
{
    private var  nextNode : Node = Node.Quit

    fun build (block : Stmt) = block.accept(this)

    override fun visitIf(x : Stmt.If) : Node {
        val saveAfterIf = nextNode
        val thenBranch = x.thenBranch.accept(this)
        nextNode = saveAfterIf
        val elseBranch =  if (x.elseBranch != null) x.elseBranch.accept(this) else saveAfterIf
        nextNode = saveAfterIf
        return Node.Condition(x.cond,  thenBranch,elseBranch, saveAfterIf)
    }

    override fun visitBlock(x: Stmt.Block) : Node  {
        x.stmts.asReversed().forEach { nextNode = it.accept(this) }
        return nextNode
    }
    override fun visitReturn(x: Stmt.Return) : Node = Node.Return(x.result)
    override fun visitAssign(x: Stmt.Assign) : Node = Node.Assign(x.variable, x.value, nextNode)
    override fun visitCycle(x: Stmt.While): Node {
        val cycle = Node.While(x.cond,Node.Quit, nextNode )
        val saveAfterBody = nextNode
        nextNode = cycle
        val cycleTemp = x.body.accept(this)
        nextNode = saveAfterBody
        cycle.body = cycleTemp

        return cycle
    }

}