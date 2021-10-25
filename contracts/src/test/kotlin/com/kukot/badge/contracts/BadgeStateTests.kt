package com.kukot.badge.contracts

import com.kukot.badge.states.TemplateState
import org.junit.Test
import kotlin.test.assertEquals

class BadgeStateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        val expectedFields = listOf("name", "badge")
        BadgeStateTests::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(TemplateState::class.java.getDeclaredField("msg").type, String()::class.java)
    }
}
