package fr.f4fez.signaling.agent

import fr.f4fez.signaling.client.AgentSessionDto
import fr.f4fez.signaling.client.ClientSignalCommand
import fr.f4fez.signaling.client.ClientSignalResponse
import fr.f4fez.signaling.client.SessionExchangeException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Component
class AgentService {
    private val sessions: MutableMap<String, AgentSession> = Collections.synchronizedMap(mutableMapOf())

    fun registerSession(session: AgentSession) {
        sessions[session.sessionId] = session
    }

    fun signalClient(clientSignalCommand: ClientSignalCommand): Mono<ClientSignalResponse> {
        return sessions[clientSignalCommand.agentSessionId]?.signalClient(clientSignalCommand)
            ?: Mono.error { SessionExchangeException("No matching session found") }

    }

    fun getSessions(): Flux<AgentSessionDto> =
        sessions.values.map { AgentSessionDto(it.agentClientDescription!!.agentName, it.sessionId) }.toFlux()

}