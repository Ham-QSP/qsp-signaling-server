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
import fr.f4fez.signaling.client.SessionNotFoundException
import fr.f4fez.signaling.error.StationGrantAccessDeniedException
import fr.f4fez.signaling.management.dal.StationGrantRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AgentService(
    val sessionsService: AgentSessionService,
    val agentSessionSocketController: AgentSessionSocketController,
    val stationGrantRepository: StationGrantRepository,
) {
    private val logger = KotlinLogging.logger {}


    fun signalClient(clientSignalCommand: ClientSignalCommand, subject: String): Mono<ClientSignalResponse> {
        logger.debug { "Client signal for agent session: ${clientSignalCommand.agentSessionId}" }
        val agentSession = sessionsService.getSession(clientSignalCommand.agentSessionId)
            ?: return Mono.error(SessionNotFoundException("No matching agent session found"))

        val stationId = agentSession.agentInformation?.stationId
            ?: return Mono.error(StationGrantAccessDeniedException("Agent session has no station information"))

        return stationGrantRepository.existsByStationIdAndSubject(stationId, subject)
            .flatMap { allowed ->
                if (!allowed) {
                    Mono.error(StationGrantAccessDeniedException("No station grant for subject on this station"))
                } else {
                    agentSessionSocketController.clientSignal(agentSession, clientSignalCommand.clientSdp)
                        .map { ClientSignalResponse(it.sdp) }
                }
            }
    }

}