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
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Service
class AgentSessionService {
    private val sessions: MutableMap<String, AgentSession> = Collections.synchronizedMap(mutableMapOf())

    private fun registerSession(session: AgentSession) {
        sessions[session.sessionId] = session
    }

    private fun unregisterSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun getSession(sessionId: String): AgentSession? = sessions[sessionId]


    fun getSessions(): Flux<AgentSessionDto> =
        sessions.values.map { AgentSessionDto(it.agentDescription!!.agentName, it.sessionId) }.toFlux()

    fun createSession(): AgentSession =
        AgentSession(
            { registerSession(it) },
            { unregisterSession(it.sessionId) },
        )
}