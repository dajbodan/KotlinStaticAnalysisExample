package org.example.analysis

import org.example.lang.ast.Expr
import org.example.lang.cfg.Node
import org.example.lang.cfg.NodeVisitor
import org.example.lang.values.*
import java.util.Collections.emptyMap

internal class ConstantFolding(val root : Node)
{


    private sealed interface AEnvVariable
    {
        data class Var(val name: String) : AEnvVariable
        data class ConstExpr(val node : Node) : AEnvVariable
    }

    private typealias Env = MutableMap<AEnvVariable, AValue<Val>>

    private val nodesOfVariable : LinkedHashMap<Node, Env> = linkedMapOf()

    private val predeccesor : LinkedHashMap<Node, MutableList<Node>> = linkedMapOf<Node, MutableList<Node>>()
    private val que = ArrayDeque<Node>()

    fun run()
    {
        initStates()
        initPredeccesor()

        var itNode = root

        que.addLast(itNode)
        val inQueue : MutableSet<Node> = mutableSetOf(itNode)
        while(!que.isEmpty())
        {
            itNode = que.removeFirst()
            inQueue.remove(itNode)
            val predecessor = predeccesor[itNode] ?: emptyList<Node>()
            val predOutStates = predecessor.map { envAt(it) }
            val inState = if (itNode == root) {
                merge(predOutStates + listOf(mutableMapOf()))
            } else {
                merge(predOutStates)
            }

            val newOutState = getOutState(itNode, inState)
            val oldOutState = nodesOfVariable[itNode]

            if(oldOutState != newOutState)
            {
                nodesOfVariable[itNode] = newOutState
                itNode.successors().forEach { if(!inQueue.contains(it)) que.addLast(it); inQueue.add(it)  }
            }
        }

    }


    fun buildNewAST() : Node = root.visit(ConstantFoldingVisitor())

    private fun getOutState(
        node : Node,
        inState : Env
    ) : Env
    {
        val outState = inState.toMutableMap()
        val evaluated = evaluate(node.expression(), inState)
        if( node is Node.Assign)
            outState[AEnvVariable.Var(node.variable.name)] = evaluated
        else
            outState[AEnvVariable.ConstExpr(node)] = evaluated
        return outState
    }
    private fun initStates() = root.forEachRecursive { node -> nodesOfVariable[node] = mutableMapOf() }

    private fun initPredeccesor()
    {
        root.forEachRecursive { node -> predeccesor[node] = mutableListOf<Node>() }
        root.forEachRecursive { node -> node.successors().forEach { successor -> predeccesor[successor]?.add(node) } }
    }

    private fun merge(states: List<Env>): Env {
        val result = mutableMapOf<AEnvVariable, AValue<Val>>()
        val allKeys = states.fold(emptySet<AEnvVariable>()) { acc, env -> acc + env.keys }
        for (key in allKeys) {
            val merged = states.map { env -> env[key] ?: AValue.Unknown }.toSet()
            result[key] = if (merged.size == 1) merged.first() else AValue.Unknown
        }
        return result
    }



    private fun evaluate(e: Expr, context : Env) : AValue<Val> = when(e) {
        is Expr.Var     -> context[AEnvVariable.Var(e.name)] ?: AValue.Unknown
        is Expr.Const   -> AValue.Const(e.value)
        is Expr.Plus    -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a + b }
        is Expr.Minus   -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a - b }
        is Expr.Mul     -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a * b }
        is Expr.Eq      -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a.Equals(b) }
        is Expr.Neq     -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a.NotEquals(b) }
        is Expr.Lt      -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a.Lt(b) }

    }


    private inline fun evaluateBinaryOp(leftExpr : Expr,
                                                  rightExpr : Expr,
                                                  context : MutableMap<AEnvVariable, AValue<Val>>,
                                                  op : (Val, Val) -> AValue<Val>) : AValue<Val>
    {

        val left = evaluate(leftExpr, context)
        val right = evaluate(rightExpr, context)
        if (left is AValue.Const && right is AValue.Const)
            return op(left.value, right.value)
        return AValue.Unknown
    }
    private fun envAt(n: Node): Env =
        nodesOfVariable[n] ?: emptyMap()

    private fun valueAt(n: Node, key: AEnvVariable): AValue<Val> =
        envAt(n)[key] ?: AValue.Unknown



    private inner class ConstantFoldingVisitor() : NodeVisitor<Node>
    {
        private val cycles = mutableMapOf<Node, Node>()

        override fun visitAssign(x: Node.Assign): Node {
            val tempValue = valueAt(x, AEnvVariable.Var(x.variable.name))
            val tempNext = x.next.visit(this)

            if (tempValue is AValue.Const)
                return Node.Assign(x.variable, Expr.Const(tempValue.value), tempNext)
            else
                return Node.Assign(x.variable, x.value, tempNext)
        }

        override fun visitCycle(x: Node.While): Node {
            cycles[x]?.let { return it }
            val tempValue = valueAt(x, AEnvVariable.ConstExpr(x))
            val tempJoin = x.next.visit(this)
            var tempCycle : Node?
            if (tempValue is AValue.Const)
                tempCycle = Node.While(Expr.Const(tempValue.value), Node.Quit, tempJoin)
            else
                tempCycle = Node.While(x.cond, Node.Quit, tempJoin)
            cycles[x] = tempCycle
            tempCycle.body = x.body.visit(this)
            return tempCycle

        }

        override fun visitConditional(x: Node.Condition): Node {
            val nextIfTrue = x.nextIfTrue.visit(this)
            val nextIfFalse = x.nextIfFalse.visit(this)
            val tempValue = valueAt(x, AEnvVariable.ConstExpr(x))

            if(tempValue is AValue.Const)
                return Node.Condition(Expr.Const(tempValue.value), nextIfTrue, nextIfFalse)
            else
                return Node.Condition(x.cond, nextIfTrue, nextIfFalse)
        }

        override fun visitQuit(x: Node.Quit): Node = Node.Quit

        override fun visitReturn(x: Node.Return): Node {

            val tempValue = valueAt(x, AEnvVariable.ConstExpr(x))
            if(tempValue is AValue.Const)
                return Node.Return(Expr.Const(tempValue.value))
            return Node.Return(x.result)

        }
    }
}