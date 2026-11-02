package fr.f4fez.signaling.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class HelloWatchdogTest {

    private val timeout = Duration.ofSeconds(10)

    private fun newSession() = AgentSession({}, {})

    private fun hello() = AgentClientDescription("type", "1", 0, 1, "name", "id", "secret", "desc")

    @Test
    fun `closes the connection when no hello is received in time`() {
        val closed = AtomicInteger()
        StepVerifier.withVirtualTime {
            helloWatchdog(newSession(), timeout) { closed.incrementAndGet(); Mono.empty() }
        }
            .expectSubscription()
            .thenAwait(timeout)
            .verifyComplete()
        assertEquals(1, closed.get())
    }

    @Test
    fun `does not close the connection when the handshake is done`() {
        val closed = AtomicInteger()
        val session = newSession().also { it.doHandshake(hello()) }
        StepVerifier.withVirtualTime {
            helloWatchdog(session, timeout) { closed.incrementAndGet(); Mono.empty() }
        }
            .expectSubscription()
            .thenAwait(timeout)
            .verifyComplete()
        assertEquals(0, closed.get())
    }
}
