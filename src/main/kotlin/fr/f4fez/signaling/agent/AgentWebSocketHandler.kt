package fr.f4fez.signaling.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

class AgentWebSocketHandler(val agentService: AgentService) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val agentSession = AgentSession(session)
        agentService.registerSession(agentSession)

        return agentSession.agentSessionSocket.webSocketPublisher
    }

}