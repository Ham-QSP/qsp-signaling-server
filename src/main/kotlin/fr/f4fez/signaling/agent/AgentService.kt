package fr.f4fez.signaling.agent

import fr.f4fez.signaling.client.ClientSignalCommand
import fr.f4fez.signaling.client.ClientSignalResponse
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AgentService {
    private val logger = KotlinLogging.logger {}
    private val sessions = mutableListOf<AgentSession>()

    fun registerSession(session: AgentSession) {
        sessions.add(session)
    }

    fun signalClient(clientSignalCommand: ClientSignalCommand): Mono<ClientSignalResponse> {
        if (sessions.size > 0) {
            sessions[0].signalClient(clientSignalCommand)
        } else {
            logger.error { "No matching agent found" }
        }
        return Mono.just(ClientSignalResponse("Server SDP"))
    }
}