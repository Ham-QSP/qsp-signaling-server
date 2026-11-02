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
package fr.f4fez.signaling.management.dto

import fr.f4fez.signaling.agent.AgentSession
import fr.f4fez.signaling.management.dal.AgentEntity
import java.util.*

data class Agent(
    val station: UUID,
    val displayName: String,
    val secret: String? = null,
    val id: UUID? = null,
    val sessionId: UUID? = null,
    val sessionName: String? = null,
    val sessionDescription: String? = null,
    val sessionVersion: String? = null,
    val sessionType: String? = null,
) {

    fun copyWithSession(session: AgentSession): Agent =
        this.copy(
            sessionName = session.agentDescription?.agentName,
            sessionDescription = session.agentDescription?.description,
            sessionVersion = session.agentDescription?.version,
            sessionType = session.agentDescription?.agentType,
            sessionId = session.sessionId
        )

}

fun AgentEntity.toDTO() = Agent(stationId, displayName, secret?.substring(0, 4) + "***", id)
fun AgentEntity.toDTOWithSecret() = Agent(stationId, displayName, secret, id)