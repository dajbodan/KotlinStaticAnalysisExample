package org.example.ConstantFolding

import org.example.Expression.Expr
import org.example.Node.Node
import org.example.Node.NodeVisitor

import kotlin.collections.plus


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
                nodesOfVariable[prevNode]!!
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
                when(itNode)
                {
                    is Node.Assign      ->   AssignHandler( itNode)
                    is Node.Return      ->   ReturnHanlder( itNode)
                    is Node.While       ->   CycleHandler( itNode)
                    is Node.Condition   ->   ConditionHandler( itNode)
                    is Node.Quit        ->  error("Error: Node.Quit is reached")
                    else -> error("Error: Missed type")
                }
            }
        }

    }


    fun buildNewAST() : Node = root.visit(ConstantFoldingVisitor())


    private fun AssignHandler ( assignNode : Node.Assign ) : Unit
    {
        if (!que.contains(assignNode.next)) que.addLast(assignNode.next)
    }

    private fun ReturnHanlder (returnNode : Node.Return ) : Unit
    {

    }

    private fun CycleHandler ( cycleNode : Node.While ) : Unit
    {
        if (!que.contains(cycleNode.body)) que.addLast(cycleNode.body)
        if (!que.contains(cycleNode.join)) que.addLast(cycleNode.join)
    }

    private fun ConditionHandler ( conditionNode : Node.Condition ) : Unit
    {
        if (!que.contains(conditionNode.nextIfTrue)) que.addLast(conditionNode.nextIfTrue)
        if (!que.contains(conditionNode.nextIfFalse)) que.addLast(conditionNode.nextIfFalse)
    }

    private fun getOutState(
        node : Node,
        inState : Env
    ) : Env
    {
        val outState = inState.toMutableMap()

        when(node){
            is Node.Assign -> {
                val evaluated = evaluate(node.value, inState)
                outState[AEnvVariable.Var(node.variable.name)] = evaluated
            }
            is Node.Return, is Node.Condition, is Node.While  -> {

                val evaluated = tryEvaluateExpr(node, inState)

                outState[AEnvVariable.ConstExpr(node)] = evaluated
            }
            else  -> {  }
        }
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
            /*result[key] = when
            {
                values.isEmpty() -> AValue.Unknown
                values.size == 1 -> values.first()
                else             -> AValue.Unknown
            }*/
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

    private fun evaluate( e: Expr, context : Env) : AValue<Any> = when(e) {
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
        private val cache = mutableMapOf<Node, Node>()

        override fun visitAssign(x: Node.Assign): Node {
            val tempValue = nodesOfVariable[x]!!.get(AEnvVariable.Var(x.variable.name))!!
            if(!cache.containsKey(x.next))
                cache[x.next] = x.next.visit(this)


            if (tempValue is AValue.Const)
                cache[x] = Node.Assign(x.variable, Expr.Const(tempValue.value), cache[x.next]!!)
            else
                cache[x] = Node.Assign(x.variable, x.value, cache[x.next]!!)
            return cache[x]!!
        }

        override fun visitCycle(X: Node.While): Node {
            val tempValue = nodesOfVariable[X]!!.get(AEnvVariable.ConstExpr(X))!!
            if (!cache.containsKey(X.join) )
                cache[X.join] = X.join.visit(this)

            if (tempValue is AValue.Const)
                cache[X] = Node.While(Expr.Const(tempValue.value), Node.Quit, cache[X.join]!!)
            else
                cache[X] = Node.While(X.cond, Node.Quit, cache[X.join]!!)

            if(!cache.containsKey(X.body) )
                cache[X.body] = X.body.visit(this)

            (cache[X]!! as Node.While).body = cache[X.body]!!

            return cache[X]!!
        }

        override fun visitConditional(x: Node.Condition): Node {
            if (!cache.containsKey(x.nextIfTrue) )
                cache[x.nextIfTrue] = x.nextIfTrue.visit(this)

            if (!cache.containsKey(x.nextIfFalse) )
                cache[x.nextIfFalse] = x.nextIfFalse.visit(this)
            if(!cache.containsKey(x.join) )
                cache[x.join] = x.join.visit(this)

            val tempValue = nodesOfVariable[x]!!.get(AEnvVariable.ConstExpr(x))!!
            if (tempValue is AValue.Const)
                cache[x] = Node.Condition(Expr.Const(tempValue.value),
                    cache[x.nextIfTrue]!!,
                    cache[x.nextIfFalse]!!,
                    cache[x.join]!!)
            else
                cache[x] = Node.Condition(x.cond,
                    cache[x.nextIfTrue]!!,
                    cache[x.nextIfFalse]!!,
                    cache[x.join]!!)
            return cache[x]!!
        }

        override fun visitQuit(x: Node.Quit): Node = Node.Quit

        override fun visitReturn(x: Node.Return): Node {
            val tempValue = nodesOfVariable[x]!!.get(AEnvVariable.ConstExpr(x))!!
            if (tempValue is AValue.Const)
                cache[x] = Node.Return(Expr.Const(tempValue.value))
            else
                cache[x] = Node.Return(x.result)
            return cache[x]!!
        }
    }


}