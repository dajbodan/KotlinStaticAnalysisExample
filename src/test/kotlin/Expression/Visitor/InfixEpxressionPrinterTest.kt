package org.example.Expression.Visitor

import org.example.Expression.Expr
import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.*

internal class InfixExpresionPrinterTest {

    private val printer = InfixExpresionPrinter()

    @Test
    fun `prints a simple constant`() {
        val ast = const(123)
        val result = printer.print(ast)
        assertEquals("123", result)
    }

    @Test
    fun `prints a simple variable`() {
        val ast = v("x")
        val result = printer.print(ast)
        assertEquals("x", result)
    }

    @Test
    fun `prints simple addition`() {
        // 1 + 2
        val ast = plus(const(1), const(2))
        val result = printer.print(ast)
        assertEquals("(1+2)", result)
    }

    @Test
    fun `prints simple multiplication`() {
        // 1 * 2
        val ast = mul(const(1), const(2))
        val result = printer.print(ast)
        assertEquals("1*2", result)
    }

    @Test
    fun `prints an equality check`() {
        // x == 1
        val ast = eq(v("x"), const(1))
        val result = printer.print(ast)
        assertEquals("x==1", result)
    }

    @Test
    fun `prints complex nested expressions correctly`() {
        // (x + 2) * (y - 3)
        val ast = mul(
            plus(v("x"), const(2)),
            minus(v("y"), const(3))
        )
        val result = printer.print(ast)
        assertEquals("(x+2)*(y-3)", result)
    }

    @Test
    fun `prints another complex expression`() {
        // 1 + (2 * 3)
        val ast = plus(
            const(1),
            mul(const(2), const(3))
        )
        val result = printer.print(ast)
        assertEquals("(1+2*3)", result)
    }
}
