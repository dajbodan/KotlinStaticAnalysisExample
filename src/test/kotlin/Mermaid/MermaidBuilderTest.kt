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

    // Helper to get the builder's output
    private val output: String
        get() = sb.toString()

    @BeforeEach
    fun setUp() {
        sb = StringBuilder()
        infixPrinter = InfixExpresionPrinter()
        builder = MermaidBuilder(
            out = sb,
            infixPrinter = infixPrinter,
            type = MermaidGraphType.GRAPH_TD // Use any type for testing
        )
    }

    @Test
    fun `init writes the graph header`() {
        // The header is written by the 'init' block.
        // We just need to check if the output contains it.
        assertTrue(output.contains("graph TD"))
    }

    @Test
    fun `addNode formats an Assign node correctly`() {
        val node = Node.Assign(Expr.Var("x"), Expr.Const(10), Node.Quit)
        builder.addNode(node)

        // We check for 'contains' instead of 'assertEquals'
        // This is less brittle and ignores the generated ID (e.g., "N0")
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

        // Add nodes first to get them into the 'nodeIds' map
        builder.addNode(n1)
        builder.addNode(n2)
        builder.addEdge(n1, n2, CFGWeights.YES)

        // The IDs will be N0 and N1
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
        // Create a simple graph: Assign -> Return
        val ret = Node.Return(Expr.Const(1))
        val assign = Node.Assign(Expr.Var("x"), Expr.Const(10), ret)

        // Run the render logic
        builder.renderFromGraph<Node, CFGWeights>(srcNode = assign) { n -> getSuccessors(node = n) }

        // Check for all parts
        // 1. Header (was in init)
        assertTrue(output.contains("graph TD"))

        // 2. Nodes
        assertTrue(output.contains("[\"Assign x = 10 \"]"))
        assertTrue(output.contains("[\"Return (1) \"]"))

        // 3. Edge
        assertTrue(output.contains("N0 --> N1"))
    }
}