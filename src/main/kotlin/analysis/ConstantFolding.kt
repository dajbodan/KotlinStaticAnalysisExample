package org.example.analysis

import org.example.lang.ast.Expr
import org.example.lang.cfg.Node
import org.example.lang.cfg.NodeVisitor

internal class ConstantFolding(val root : Node)
{
    private sealed interface AValue<out T> {
        data class Const<T>(val value: T) : AValue<T>
        object Unknown : AValue<Nothing>
    }

    private sealed interface AEnvVariable
    {
        data class Var(val name: String) : AEnvVariable
        data class ConstExpr(val node : Node) : AEnvVariable
    }

    private typealias Env = MutableMap<AEnvVariable, AValue<Any>>

    private val nodesOfVariable : LinkedHashMap<Node, Env> = linkedMapOf()

    private val predeccesor : LinkedHashMap<Node, MutableList<Node>> = linkedMapOf<Node, MutableList<Node>>()
    private val que = ArrayDeque<Node>()

    fun run()
    {
        initStates()
        initPredeccesor()

        var itNode = root

        que.addLast(itNode)

        while(!que.isEmpty())
        {
            itNode = que.removeFirst()

            val predecessor = predeccesor[itNode] ?: emptyList<Node>()

            val predOutStates = predecessor.map { prevNode ->
                nodesOfVariable.getValue(prevNode)
            }


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
                itNode.successors().forEach { if(!que.contains(it)) que.addLast(it) }
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
    private fun initStates()
    {
        root.forEachRecursive { node -> nodesOfVariable[node] = mutableMapOf() }
    }

    private fun initPredeccesor()
    {
        root.forEachRecursive { node -> predeccesor[node] = mutableListOf<Node>() }

        root.forEachRecursive { node -> node.successors().forEach { successor -> predeccesor[successor]?.add(node) } }
    }

    private fun merge( states : List<Env>) : Env
    {
        var result = mutableMapOf<AEnvVariable, AValue<Any>>()
        val allKeys = states.fold(setOf<AEnvVariable>()) { acc, env -> acc + env.keys }
        for(key in allKeys)
        {
            val values = states.mapNotNull { it[key] }.toSet()
            result[key] = if (values.size == 1) {
                values.first() ?: AValue.Unknown
            } else {
                AValue.Unknown
            }
        }

        return result
    }


    private fun tryEvaluateExpr(node : Node, context : Env) : AValue<Any>
    {
        return when(node)
        {
            is Node.Assign -> evaluate(node.value, context)
            is Node.Return -> evaluate(node.result, context)
            is Node.Condition -> evaluate(node.cond, context)
            is Node.While -> evaluate(node.cond, context)
            else -> AValue.Unknown
        }
    }

    private fun evaluate(e: Expr, context : Env) : AValue<Any> = when(e) {
        is Expr.Var     -> context[AEnvVariable.Var(e.name)] ?: AValue.Unknown
        is Expr.Const   -> AValue.Const(e.value)
        is Expr.Plus    -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a + b }
        is Expr.Minus   -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a - b }
        is Expr.Mul     -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> a * b }
        is Expr.Eq      -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> if (a == b) true else false }
        is Expr.Neq     -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> if (a != b) true else false }
        is Expr.Lt      -> evaluateBinaryOp(e.left, e.right, context)   { a, b -> if (a < b)  true else false }
        else -> error("Missed type in Expr")
    }


    private inline fun <R : Any> evaluateBinaryOp(leftExpr : Expr,
                                                  rightExpr : Expr,
                                                  context : MutableMap<AEnvVariable, AValue<Any>>,
                                                  op : (Int, Int) -> R ) : AValue<Any>
    {

        val left = evaluate(leftExpr, context)
        val right = evaluate(rightExpr, context)
        if (left is AValue.Const && right is AValue.Const && left.value is Int && right.value is Int) {
            return AValue.Const(op(left.value, right.value))
        }
        return AValue.Unknown
    }

    private inner class ConstantFoldingVisitor() : NodeVisitor<Node>
    {
        private val cycles = mutableMapOf<Node, Node>()

        override fun visitAssign(x: Node.Assign): Node {
            val tempValue = nodesOfVariable.getValue(x).getValue(AEnvVariable.Var(x.variable.name))
            val tempNext = x.next.visit(this)

            if (tempValue is AValue.Const)
                return Node.Assign(x.variable, Expr.Const(tempValue.value), tempNext)
            else
                return Node.Assign(x.variable, x.value, tempNext)
        }

        override fun visitCycle(X: Node.While): Node {
            if( cycles.contains(X))
                return cycles.getValue(X)
            val tempValue = nodesOfVariable.getValue(X).getValue(AEnvVariable.ConstExpr(X))
            val tempJoin = X.join.visit(this)
            var tempCycle : Node? = null
            if (tempValue is AValue.Const)
                tempCycle = Node.While(Expr.Const(tempValue.value), Node.Quit, tempJoin)
            else
                tempCycle = Node.While(X.cond, Node.Quit, tempJoin)
            cycles[X] = tempCycle
            tempCycle.body = X.body.visit(this)
            return tempCycle

        }

        override fun visitConditional(x: Node.Condition): Node {
            val nextIfTrue = x.nextIfTrue.visit(this)
            val nextIfFalse = x.nextIfFalse.visit(this)
            val tempValue = nodesOfVariable.getValue(x).getValue(AEnvVariable.ConstExpr(x))
            val tempJoin = x.join.visit(this)
            if(tempValue is AValue.Const)
                return Node.Condition(Expr.Const(tempValue.value), nextIfTrue, nextIfFalse, tempJoin)
            else
                return Node.Condition(x.cond, nextIfTrue, nextIfFalse, tempJoin)
        }

        override fun visitQuit(x: Node.Quit): Node = Node.Quit

        override fun visitReturn(x: Node.Return): Node {

            val tempValue = nodesOfVariable.getValue(x).getValue(AEnvVariable.ConstExpr(x))
            if(tempValue is AValue.Const)
                return Node.Return(Expr.Const(tempValue.value))
            return Node.Return(x.result)

        }
    }

}