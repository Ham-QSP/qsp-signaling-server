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

import fr.f4fez.signaling.client.ClientSignalCommand
import fr.f4fez.signaling.client.ClientSignalResponse
import mu.KotlinLogging
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Consumer

class AgentSession(
    onHandshakeDone: Consumer<AgentSession>,
    onConnectionEnd: Consumer<AgentSession>,
) {
    var agentClientDescription: AgentClientDescription? = null
    private val logger = KotlinLogging.logger {}
    val agentSessionSocket: AgentSessionSocket
    val sessionId: String

    init {
        this.agentSessionSocket = AgentSessionSocket(this, onHandshakeDone, onConnectionEnd)
        this.sessionId = UUID.randomUUID().toString()
    }

    fun signalClient(clientSignalCommand: ClientSignalCommand): Mono<ClientSignalResponse> {
        logger.debug { "{$sessionId} Send client init" }
        return agentSessionSocket.clientSignal(clientSignalCommand.clientSdp).map { ClientSignalResponse(it.sdp) }
    }

}