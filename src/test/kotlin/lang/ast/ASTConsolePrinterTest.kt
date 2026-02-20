package org.example.lang.ast

import org.example.block
import org.example.const
import org.example.lang.ast.print.ASTConsolePrinter
import org.example.lang.values.Val
import org.example.plus
import org.example.v
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ASTConsolePrinterTest {

    @Test
    fun `prints a simple assignment`() {
        val sb = StringBuilder()
        val printer = ASTConsolePrinter(sb)
        val ast = block(
            Stmt.Assign(v("x"), const(Val.IntV(10)))
        )

        ast.accept(printer)
        val output = sb.toString()

        val expected = """
            Block
            	Assign x
            		Const(10)
        """.trimIndent() + "\n"

        Assertions.assertEquals(expected, output)
    }

    @Test
    fun `prints a complex If statement`() {

        val sb = StringBuilder()
        val printer = ASTConsolePrinter(sb)
        val ast = block(
            Stmt.If(
                const(Val.BoolV(true)),
                Stmt.Assign(v("y"), const(Val.IntV(1))),
                Stmt.Assign(v("z"), plus(v("a"), const(Val.IntV(2))))
            )
        )
        ast.accept(printer)
        val output = sb.toString()

        val expected = """
            Block
            	If
            		Const(true)
            		Then
            			Assign y
            				Const(1)
            		Else
            			Assign z
            				Plus(Var(a), Const(2))
        """.trimIndent() + "\n"

        Assertions.assertEquals(expected, output)
    }
}