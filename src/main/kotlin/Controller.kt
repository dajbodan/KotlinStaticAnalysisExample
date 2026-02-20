package org.example

import org.example.analysis.ConstantFolding
import org.example.infra.graph.CFGWeights
import org.example.infra.mermaid.MermaidBuilder
import org.example.infra.mermaid.MermaidGraphType
import org.example.lang.ast.Stmt
import org.example.lang.ast.print.ASTConsolePrinter
import org.example.lang.ast.print.InfixExpresionPrinter
import org.example.lang.cfg.ControlFlowGraphBuilder
import org.example.lang.cfg.Node
//import org.example.lang.cfg.getSuccessors

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
        mermaid.renderFromGraph(entry,  {
            n -> n.successors().map { succ ->
                when (n) {
                    is Node.Assign    -> succ to null
                    is Node.While     -> succ to if(succ == n.body) CFGWeights.YES else CFGWeights.NO
                    is Node.Condition -> succ to if(succ == n.nextIfTrue) CFGWeights.YES else CFGWeights.NO
                    is Node.Return    -> succ to null
                    is Node.Quit      -> succ to null
                }
            }
        })

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