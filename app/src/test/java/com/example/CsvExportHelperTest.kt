package com.example

import com.example.util.CsvExportHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportHelperTest {
    @Test
    fun csvQuotingPreservesQuotesAndNewlines() {
        assertEquals("\"A\"\"B\\nC\"".replace("\\n", "\n"), CsvExportHelper.csvCell("A\"B\nC"))
        assertEquals("\"simple\"", CsvExportHelper.csvCell("simple"))
    }

    @Test
    fun spreadsheetFormulaPrefixesAreNeutralized() {
        for (raw in listOf("=2+3", "+967770000000", "-3+5", "@SUM(1,1)", "  =2+3")) {
            val cell = CsvExportHelper.csvCell(raw)
            assertEquals('"', cell.first())
            assertEquals('"', cell.last())
            assertEquals("'${raw}", cell.substring(1, cell.length - 1))
        }
    }
}
