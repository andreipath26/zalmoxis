package com.zalmoxis.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure policy tests; SQLite persistence requires an Android instrumented test. */
class MemoryScopePolicyTest {
    @Test fun scopesMustBeExplicitAndBounded() {
        assertTrue(MemoryScopePolicy.validScopePart("local-owner"))
        assertTrue(MemoryScopePolicy.validScopePart("client_1.org"))
        assertFalse(MemoryScopePolicy.validScopePart(""))
        assertFalse(MemoryScopePolicy.validScopePart("../client"))
        assertFalse(MemoryScopePolicy.validScopePart(" other-user"))
        assertFalse(MemoryScopePolicy.validScopePart("x".repeat(121)))
    }

    @Test fun factsMustBeNonblankAndBounded() {
        assertTrue(MemoryScopePolicy.validFactText("User-approved preference"))
        assertFalse(MemoryScopePolicy.validFactText(""))
        assertFalse(MemoryScopePolicy.validFactText(" "))
        assertFalse(MemoryScopePolicy.validFactText("a".repeat(4001)))
    }
}
