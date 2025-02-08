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

import fr.f4fez.signaling.ServerDescription
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

class AgentWebSocketHandler(
    private val agentService: AgentService,
    private val serverDescription: ServerDescription,
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val agentSession = AgentSession(
            { agentService.registerSession(it) },
            { agentService.unregisterSession(it.sessionId) },
        )
        return agentSession.agentSessionSocket.startSession(session, agentSession, serverDescription)
    }

}