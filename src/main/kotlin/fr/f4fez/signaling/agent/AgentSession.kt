package fr.f4fez.signaling.agent

import fr.f4fez.signaling.client.ClientSignalCommand
import fr.f4fez.signaling.client.ClientSignalResponse
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Consumer

class AgentSession(session: WebSocketSession, handshakeDone: Consumer<AgentSession>) {
    var agentClientDescription: AgentClientDescription? = null
    private val logger = KotlinLogging.logger {}
    val agentSessionSocket: AgentSessionSocket
    val sessionId: String

    init {
        this.agentSessionSocket = AgentSessionSocket(session, this, handshakeDone)
        this.sessionId = UUID.randomUUID().toString()
    }

    fun signalClient(clientSignalCommand: ClientSignalCommand): Mono<ClientSignalResponse> {
        logger.debug { "[${this.agentClientDescription?.agentName}] Generate client signal message" }
        return agentSessionSocket.clientSignal(clientSignalCommand.clientSdp).map { ClientSignalResponse(it) }
    }

}