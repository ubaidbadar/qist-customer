package pk.qist.customer.documents

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class DocumentsTest {
    @Test fun `uploading and staff acceptance remain separate states`() {
        val item = parseDocument(Json.parseToJsonElement("""{
          "_id":"0123456789abcdef01234567","category":"warranty","processing":"processing",
          "review":"pending","feedback":"","version":2,"canView":false,"canDelete":true
        }""").jsonObject)
        assertEquals("Checking file", item.status)
        assertFalse(item.canView)
        assertEquals("Pending staff review", item.copy(processing = "ready").status)
        assertEquals("Accepted by staff", item.copy(processing = "ready", review = "accepted").status)
        assertEquals("Needs replacement", item.copy(processing = "ready", review = "rejected").status)
        assertEquals("Replaced", item.copy(processing = "ready", review = "superseded").status)
    }
    @Test fun `metadata preserves retailer disclosure and specific request without leaking guarantor identity`() {
        val result = parseDocuments(Json.parseToJsonElement("""{
          "items":[],"requests":[{"_id":"request","category":"guarantor_cnic_front","message":"With permission"}],
          "nextCursor":null,"canUpload":true,"policy":{"maxFileBytes":10485760,"retentionNotice":"Retailer retention notice"}
        }""").jsonObject)
        assertEquals(10485760L, result.maxBytes)
        assertEquals("Retailer retention notice", result.notice)
        assertEquals("request", result.requests.single().id)
        assertNull(result.next)
        assertTrue(result.canUpload)
    }
    @Test fun `queued upload scope changes with the session and contains no bearer value`() {
        val scope = sessionScope("synthetic-session-a")
        assertEquals(64, scope.length)
        assertEquals(scope, sessionScope("synthetic-session-a"))
        assertNotEquals(scope, sessionScope("synthetic-session-b"))
        assertFalse(scope.contains("synthetic"))
    }
    @Test fun `malformed metadata cannot silently enable uploads or inflate file size`() {
        val result = parseDocuments(Json.parseToJsonElement("""{
          "items":[],"requests":[],"policy":{"maxFileBytes":999999999,"retentionNotice":"Notice"}
        }""").jsonObject)
        assertFalse(result.canUpload)
        assertEquals(10485760L, result.maxBytes)
    }
}
