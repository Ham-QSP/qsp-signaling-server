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
package fr.f4fez.signaling.management

import fr.f4fez.signaling.management.dal.AgentEntity
import fr.f4fez.signaling.management.dal.AgentRepository
import fr.f4fez.signaling.management.dal.StationRepository
import fr.f4fez.signaling.management.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.SecureRandom
import java.util.*


@Service
class StationService(
    val stationRepository: StationRepository,
    val agentRepository: AgentRepository,
    @Value("\${agent.hmac.keyLength}")
    val hmacKeyLength: Int,
) {
    private val secureRandom: SecureRandom = SecureRandom()

    fun deleteStation(id: UUID): Mono<Void> {
        return stationRepository.deleteById(id)
    }

    fun getStations(): Flux<Station> =
        stationRepository.findAll().map { s -> s.toDTO() }

    fun createStation(station: StationUpdate): Mono<Station> =
        stationRepository.save(station.toEntity()).map { s -> s.toDTO() }

    fun getAgents(stationId: UUID): Flux<Agent> =
        agentRepository.findByStationId(stationId).map { a -> a.toDTO() }

    fun registerAgent(stationId: UUID, agent: AgentUpdate): Mono<Agent> {
        val secretBytes = ByteArray(hmacKeyLength)
        secureRandom.nextBytes(secretBytes)
        val entity = AgentEntity(stationId, agent.displayName, secretBytes.toHex())
        return agentRepository.save(entity).map { a -> a.toDTOWithSecret() }
    }

    fun deleteAgent(stationId: UUID, agentId: UUID): Mono<Void> =
        agentRepository.deleteByIdAndStationId(agentId, stationId)

    fun generateAgentSecret(stationId: UUID, agentId: UUID): Mono<Agent> =
        agentRepository.findByIdAndStationId(agentId, stationId)
            .map { a -> generateSecretForAgent(a) }
            .flatMap { a -> agentRepository.save(a) }
            .map { a -> a.toDTOWithSecret() }


    private fun generateSecretForAgent(a: AgentEntity): AgentEntity {
        val secretBytes = ByteArray(hmacKeyLength)
        secureRandom.nextBytes(secretBytes)
        a.secret = secretBytes.toHex()
        return a
    }
}

fun ByteArray.toHex(): String = HexFormat.of().formatHex(this)
