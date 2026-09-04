package com.example

import com.example.data.database.entity.BlockedPatternEntity
import com.example.util.BlockedPatternHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BlockedPatternHelperTest {

    @Test
    fun testStartsWithPatternMatchesPrefix() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = "+96770",
                matchType = BlockedPatternEntity.MATCH_STARTS_WITH,
                label = "Internal staff",
                isEnabled = true
            )
        )

        val match = BlockedPatternHelper.findMatchingPattern("+967701234567", patterns)
        assertNotNull(match)
        assertEquals("+96770", match?.pattern)
        assertEquals("Internal staff", match?.label)
    }

    @Test
    fun testStartsWithDoesNotMatchDifferentPrefix() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = "+96770",
                matchType = BlockedPatternEntity.MATCH_STARTS_WITH,
                isEnabled = true
            )
        )

        val match = BlockedPatternHelper.findMatchingPattern("+967771234567", patterns)
        assertNull(match)
    }

    @Test
    fun testDisabledPatternIsIgnored() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = "+96770",
                matchType = BlockedPatternEntity.MATCH_STARTS_WITH,
                isEnabled = false
            )
        )

        val match = BlockedPatternHelper.findMatchingPattern("+967701234567", patterns)
        assertNull(match)
    }

    @Test
    fun testContainsPattern() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = "4444",
                matchType = BlockedPatternEntity.MATCH_CONTAINS,
                label = "Spam block",
                isEnabled = true
            )
        )

        assertNotNull(BlockedPatternHelper.findMatchingPattern("+967774444555", patterns))
        assertNull(BlockedPatternHelper.findMatchingPattern("+967771234567", patterns))
    }

    @Test
    fun testExactMatchPattern() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = "+967771234567",
                matchType = BlockedPatternEntity.MATCH_EXACT,
                label = "Known spammer",
                isEnabled = true
            )
        )

        assertNotNull(BlockedPatternHelper.findMatchingPattern("+967771234567", patterns))
        assertNull(BlockedPatternHelper.findMatchingPattern("+967771234568", patterns))
    }

    @Test
    fun testRegexPattern() {
        val patterns = listOf(
            BlockedPatternEntity(
                id = 1,
                pattern = """^(\+967)?70\d{7}$""",
                matchType = BlockedPatternEntity.MATCH_REGEX,
                label = "Regex Carrier Rule",
                isEnabled = true
            )
        )

        assertNotNull(BlockedPatternHelper.findMatchingPattern("+967701234567", patterns))
        assertNull(BlockedPatternHelper.findMatchingPattern("+967771234567", patterns))
    }
}
