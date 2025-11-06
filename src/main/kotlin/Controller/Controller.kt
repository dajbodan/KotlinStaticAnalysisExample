package org.example.Controller



import org.example.ASTConsolePrinter.ASTConsolePrinter
import org.example.BuilderControlFlowGraph.ControlFlowGraphBuilder
import org.example.ConstantFolding.ConstantFolding
import org.example.Mermaid.MermaidBuilder
import org.example.Mermaid.MermaidGraphType
//import org.example.Mermaid.RenderGraphIntoMermaid
import org.example.Node.Node
import org.example.Node.getSuccessors
import org.example.Statement.Stmt
import org.example.Expression.Visitor.InfixExpresionPrinter

class Controller(
    private val graphType: MermaidGraphType = MermaidGraphType.GRAPH_TD
) {

    private fun buildCfg(program: Stmt): Node {
        val builder = ControlFlowGraphBuilder()
        return program.accept(builder)
    }


    private fun renderMermaid(entry: Node): String {
        val sb = StringBuilder()
        val mermaid = MermaidBuilder(sb, InfixExpresionPrinter(), graphType)
        mermaid.renderFromGraph(entry,  { n -> getSuccessors(n) })

        return sb.toString()
    }


    private fun constantFold(entry: Node): Node {
        val pass = ConstantFolding(entry)
        pass.run()
        return pass.buildNewAST()
    }


    fun printAst(program: Stmt) {
        program.accept(ASTConsolePrinter())
        println()
    }


    internal data class PipelineResult(
        val cfgMermaid: String,
        val optimizedCfgMermaid: String,
        val cfgEntry: Node,
        val optimizedEntry: Node
    )

    internal fun run(program: Stmt): PipelineResult {
        val cfg = buildCfg(program)
        val cfgMermaid = renderMermaid(cfg)
        val optimized = constantFold(cfg)
        val optimizedMermaid = renderMermaid(optimized)
        return PipelineResult(cfgMermaid, optimizedMermaid, cfg, optimized)
    }



}
