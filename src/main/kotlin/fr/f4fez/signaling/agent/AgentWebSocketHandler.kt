package fr.f4fez.signaling.agent

import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

class AgentWebSocketHandler(private val agentService: AgentService) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val agentSession = AgentSession(session) { agentService.registerSession(it) } //FIXME remove when disconnected
        return agentSession.agentSessionSocket.webSocketPublisher
    }

}