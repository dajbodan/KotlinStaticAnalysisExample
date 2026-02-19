package org.example


import org.example.lang.ast.print.ASTConsolePrinter
import java.io.File

import org.example.Controller
import org.example.lang.ast.Expr
import org.example.lang.ast.Stmt


fun printAst(program: Stmt) {
    program.accept(ASTConsolePrinter())
    println()
}

fun sampleProgram(): Stmt =
    Stmt.Block(
        listOf(Stmt.Assign(Expr.Var("x"), Expr.Const(125)),
            Stmt.Assign(Expr.Var("a"), Expr.Mul(Expr.Minus(Expr.Const(1), Expr.Const(2)), Expr.Plus(Expr.Var("x"), Expr.Const(1)))),
            Stmt.While(Expr.Eq(Expr.Plus(Expr.Var("x"), Expr.Const(2)), Expr.Plus(Expr.Var("a"), Expr.Const(5)))
                ,Stmt.If(
                    Expr.Var("a"),
                    Stmt.Assign(Expr.Var("y"), Expr.Mul(Expr.Const(1), Expr.Const(1))),
                    Stmt.Assign(Expr.Var("y"), Expr.Const(1))
                )),

            Stmt.Return(
                Expr.Plus(
                    Expr.Mul(Expr.Var("x"), Expr.Const(2)),
                    Expr.Var("y")
                )
            )
        )
    )




fun main() {
    println("PROJECT DEMONSTRATION: CONSTANT FOLDING")

    val prog = sampleProgram()
    println("1. ORIGINAL AST")
    printAst(prog)
    println("\n")

    val c = Controller()
    val res = c.run(prog)

    println("2. ORIGINAL CFG (MERMAID)")
    println(res.cfgMermaid + "\n")


    println("3. OPTIMIZED CFG (MERMAID)")
    println(res.optimizedCfgMermaid + "\n")


    println("DEMONSTRATION COMPLETE")

    File("example.mmd").writeText(res.cfgMermaid)
    File("example_optimized.mmd").writeText(res.optimizedCfgMermaid)
    println("Mermaid files also written to disk.")
}