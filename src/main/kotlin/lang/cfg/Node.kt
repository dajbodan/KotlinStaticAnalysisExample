package org.example.lang.cfg

import org.example.lang.ast.Expr

sealed interface Node
{
    fun successors(): List<Node>
    fun expression(): Expr
    fun forEachRecursive(action: (Node) -> Unit)
    {
        val seen = HashSet<Node>()
        fun dfs(node : Node)
        {
            if(! seen.add(node)) return
            action(node)
            node.successors().forEach { dfs(it) }
        }
        dfs(this)
    }
    fun <T>visit(visitor : NodeVisitor<T>) :T
    class Assign(
        val variable : Expr.Var,
        val value : Expr,
        val next: Node
    ) : Node {
        override fun successors(): List<Node> = listOf(next)
        override fun expression(): Expr = value
        override fun <T> visit(visitor: NodeVisitor<T>) = visitor.visitAssign(this)
    }

    class Return(val result : Expr) : Node {
        override fun successors(): List<Node> = listOf()
        override fun expression() = result
        override fun <T> visit(visitor: NodeVisitor<T>) = visitor.visitReturn(this)
    }

    class Condition(
        val cond : Expr,
        val nextIfTrue : Node,
        val nextIfFalse : Node
    ) : Node {
        override fun successors(): List<Node> = listOf(nextIfTrue, nextIfFalse)
        override fun expression() = cond
        override fun <T> visit(visitor: NodeVisitor<T>) = visitor.visitConditional(this)
    }

    class While(
        val cond : Expr,
        var body : Node,
        val next: Node
    ) : Node {
        override fun successors(): List<Node> = listOf(body, next)
        override fun expression() = cond
        override fun <T> visit(visitor: NodeVisitor<T>) = visitor.visitCycle(this)
    }

    object Quit: Node {
        override fun successors(): List<Node> = listOf()
        override fun expression(): Expr = throw UnsupportedOperationException("Quit node does not have an expression")
        override fun <T> visit(visitor: NodeVisitor<T>) = visitor.visitQuit(this)
    }
}
