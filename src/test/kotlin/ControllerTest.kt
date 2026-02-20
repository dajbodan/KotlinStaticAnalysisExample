package org.example

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ControllerTest {

    @Test
    fun `pipeline runs without crashing and produces an optimized result`() {
        val controller = Controller()
        val program = sampleProgram()

        val result = controller.run(program)

        assertTrue(result.cfgMermaid.isNotEmpty())
        assertTrue(result.optimizedCfgMermaid.isNotEmpty())

        assertNotEquals(
            result.cfgMermaid,
            result.optimizedCfgMermaid
        )

        assertTrue(
            result.optimizedCfgMermaid.contains("Assign a = -126")
        )
    }
}