package org.example.Mermaid

import org.example.Expression.Visitor.InfixExpresionPrinter
import org.example.Node.*
import org.example.OrientedGraph.CFGWeights
import org.example.OrientedGraph.OrientedGraph


internal class MermaidBuilder(
    private val out: Appendable,
    val infixPrinter : InfixExpresionPrinter,
    type : MermaidGraphType
) : OrientedGraph<Node, CFGWeights>
{
    private val nodeIds = mutableMapOf<Node, String>()
    private var counter = 0
    private fun getNodeId(node: Node) = nodeIds.computeIfAbsent(node) { "N${counter++}" }

    init {
        out.append(type.header).append("\n")
    }

    override fun addNode(x: Node) {
        require(! nodeIds.containsKey(x))
        val id = getNodeId(x)
        val label = when (x) {
            is Node.Assign -> "Assign ${x.variable.name} = ${infixPrinter.print(x.value)}"
            is Node.Condition -> "If (${infixPrinter.print(x.cond)})"
            is Node.Return -> "Return (${infixPrinter.print(x.result)})"
            is Node.While -> "While (${infixPrinter.print(x.cond)})"
            is Node.Quit -> "Quit"
        }

        out.appendLine("$id[\"$label \"]")
    }

    override fun addEdge(from: Node, to: Node, weight: CFGWeights?) {
        require(nodeIds.containsKey(from) and nodeIds.containsKey(to))

        val fromId = getNodeId(from)
        val toId = getNodeId(to)

        val label = when (weight) {
            CFGWeights.YES -> "|Yes|"
            CFGWeights.NO -> "|No|"
            null -> ""
        }

        out.appendLine("    $fromId -->$label $toId")
    }

    fun <N, W> renderFromGraph(
        srcNode: N,
        graphAlgorithm: (N) -> List<Pair<N, W?>>
    ) where N : Node, W : CFGWeights? {

        val visited = mutableSetOf<N>()

        fun generateVertices(node: N) {
            if (!visited.contains(node)) {
                visited.add(node)
                // 'this.addNode' or just 'addNode'
                addNode(node)
                graphAlgorithm(node).forEach { generateVertices(it.first) }
            }
        }

        fun generateEdges(node: N) {
            if (!visited.contains(node)) {
                visited.add(node)
                graphAlgorithm(node).forEach {

                    addEdge(node, it.first, it.second)
                    generateEdges(it.first)
                }
            }
        }

        generateVertices(srcNode)
        visited.clear()
        generateEdges(srcNode)
        visited.clear()
    }


}

