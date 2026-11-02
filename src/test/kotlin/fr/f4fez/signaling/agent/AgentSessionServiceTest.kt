package fr.f4fez.signaling.agent

import fr.f4fez.signaling.management.dal.StationGrantRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class AgentSessionServiceTest {

    private val service = AgentSessionService(mock(StationGrantRepository::class.java), 2)

    private fun hello() = AgentClientDescription("type", "1", 0, 1, "name", "id", "secret", "desc")

    @Test
    fun `rejects sessions above the pending connections limit`() {
        assertNotNull(service.createSession())
        assertNotNull(service.createSession())
        assertNull(service.createSession())
        assertEquals(2, service.pendingConnectionCount)
    }

    @Test
    fun `handshake and connection end release the pending slot once`() {
        val first = service.createSession()!!
        val second = service.createSession()!!

        first.doHandshake(hello())
        assertEquals(1, service.pendingConnectionCount)

        first.onConnectionEnd.accept(first)
        second.onConnectionEnd.accept(second)
        second.onConnectionEnd.accept(second)
        assertEquals(0, service.pendingConnectionCount)
        assertNotNull(service.createSession())
    }
}
