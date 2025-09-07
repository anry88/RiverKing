package app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BotRoutesTest {
    @Test
    fun `parse new format invoice payload`() {
        val payload = "pack=abc;user=123"
        val result = parseInvoicePayload(payload, 123)
        assertEquals("abc", result)
    }

    @Test
    fun `parse old format invoice payload`() {
        val payload = "pack_abc"
        val result = parseInvoicePayload(payload, 123)
        assertEquals("abc", result)
    }

    @Test
    fun `reject mismatched user in payload`() {
        val payload = "pack=abc;user=456"
        val result = parseInvoicePayload(payload, 123)
        assertNull(result)
    }
}
