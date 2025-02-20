/*
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License,
or (at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package fr.f4fez.signaling.configuration

import fr.f4fez.signaling.agent.AgentService
import fr.f4fez.signaling.agent.AgentSessionSocketController
import fr.f4fez.signaling.agent.AgentWebSocketHandler
import fr.f4fez.signaling.server.ServerDescription
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
class AgentWebSocketConfiguration(val agentService: AgentService) {
    @Bean
    fun handlerMapping(serverDescription: ServerDescription, agentSessionSocketController: AgentSessionSocketController): HandlerMapping {
        val map = mapOf("/server/session" to AgentWebSocketHandler(serverDescription, agentSessionSocketController))
        val order = -1 // before annotated controllers

        return SimpleUrlHandlerMapping(map, order)
    }

    @Bean
    fun serverDescription(@Value("\${qsp.serverName}") name: String, @Value("\${qsp.serverDescription}") description: String): ServerDescription =
        ServerDescription(name, description)
}