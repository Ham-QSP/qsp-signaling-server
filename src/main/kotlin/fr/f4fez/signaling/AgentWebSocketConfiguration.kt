package fr.f4fez.signaling

import fr.f4fez.signaling.agent.AgentService
import fr.f4fez.signaling.agent.AgentWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class AgentWebSocketConfiguration(val agentService: AgentService) {
    @Bean
    fun handlerMapping(): HandlerMapping {
        val map = mapOf("/server/session" to AgentWebSocketHandler(agentService))
        val order = -1 // before annotated controllers

        return SimpleUrlHandlerMapping(map, order)
    }
}