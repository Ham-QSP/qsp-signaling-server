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

package fr.f4fez.signaling.agent

import fr.f4fez.signaling.client.AgentSessionDto
import fr.f4fez.signaling.client.ClientSignalCommand
import fr.f4fez.signaling.client.ClientSignalResponse
import fr.f4fez.signaling.client.SessionNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Component
class AgentService {
    private val logger = KotlinLogging.logger {}
    private val sessions: MutableMap<String, AgentSession> = Collections.synchronizedMap(mutableMapOf())

    fun registerSession(session: AgentSession) {
        sessions[session.sessionId] = session
    }

    fun unregisterSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun signalClient(clientSignalCommand: ClientSignalCommand): Mono<ClientSignalResponse> {
        logger.debug("Client signal for agent session: ${clientSignalCommand.agentSessionId}")
        return sessions[clientSignalCommand.agentSessionId]?.signalClient(clientSignalCommand)
            ?: Mono.error { SessionNotFoundException("No matching agent session found") }

    }

    fun getSessions(): Flux<AgentSessionDto> =
        sessions.values.map { AgentSessionDto(it.agentClientDescription!!.agentName, it.sessionId) }.toFlux()

}