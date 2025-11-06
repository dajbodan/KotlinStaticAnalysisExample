package org.example.Mermaid

import org.example.Expression.Expr
import org.example.Expression.Visitor.InfixExpresionPrinter
import org.example.Node.*
import org.example.OrientedGraph.CFGWeights
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class MermaidBuilderTest {

    private lateinit var sb: StringBuilder
    private lateinit var infixPrinter: InfixExpresionPrinter
    private lateinit var builder: MermaidBuilder

    private val output: String
        get() = sb.toString()

    @BeforeEach
    fun setUp() {
        sb = StringBuilder()
        infixPrinter = InfixExpresionPrinter()
        builder = MermaidBuilder(
            out = sb,
            infixPrinter = infixPrinter,
            type = MermaidGraphType.GRAPH_TD
        )
    }

    @Test
    fun `init writes the graph header`() {
        assertTrue(output.contains("graph TD"))
    }

    @Test
    fun `addNode formats an Assign node correctly`() {
        val node = Node.Assign(Expr.Var("x"), Expr.Const(10), Node.Quit)
        builder.addNode(node)
        assertTrue(output.contains("[\"Assign x = 10 \"]"))
    }

    @Test
    fun `addNode formats a Return node correctly`() {
        val node = Node.Return(Expr.Const(123))
        builder.addNode(node)
        assertTrue(output.contains("[\"Return (123) \"]"))
    }

    @Test
    fun `addEdge formats a 'Yes' edge correctly`() {
        val n1 = Node.Quit
        val n2 = Node.Return(Expr.Const(1))

        builder.addNode(n1)
        builder.addNode(n2)
        builder.addEdge(n1, n2, CFGWeights.YES)

        assertTrue(output.contains("N0 -->|Yes| N1"))
    }

    @Test
    fun `addEdge formats a 'No' edge correctly`() {
        val n1 = Node.Quit
        val n2 = Node.Return(Expr.Const(1))

        builder.addNode(n1)
        builder.addNode(n2)
        builder.addEdge(n1, n2, CFGWeights.NO)

        assertTrue(output.contains("N0 -->|No| N1"))
    }

    @Test
    fun `renderFromGraph traverses and builds a full graph`() {
        val ret = Node.Return(Expr.Const(1))
        val assign = Node.Assign(Expr.Var("x"), Expr.Const(10), ret)

        builder.renderFromGraph<Node, CFGWeights>(srcNode = assign) { n -> getSuccessors(node = n) }

        assertTrue(output.contains("graph TD"))

        assertTrue(output.contains("[\"Assign x = 10 \"]"))
        assertTrue(output.contains("[\"Return (1) \"]"))
        
        assertTrue(output.contains("N0 --> N1"))
    }
}
