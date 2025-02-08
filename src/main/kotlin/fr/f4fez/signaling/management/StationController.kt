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

import fr.f4fez.signaling.management.dto.Agent
import fr.f4fez.signaling.management.dto.AgentUpdate
import fr.f4fez.signaling.management.dto.StationUpdate
import fr.f4fez.signaling.management.dto.Station
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/station")
class StationController(val stationService: StationService) {

    @GetMapping
    fun getStation(): Flux<Station> = stationService.getStations()

    @PostMapping
    fun createStation(@RequestBody station: StationUpdate): Mono<Station> = stationService.createStation(station)

    @DeleteMapping("/{id}")
    fun deleteStation(@PathVariable("id") id: UUID): Mono<Void> = stationService.deleteStation(id)

    @GetMapping("/{stationId}/agent")
    fun getAgents(@PathVariable("stationId")stationId: UUID): Flux<Agent> = stationService.getAgents(stationId)

    @PostMapping("/{stationId}/agent")
    fun registerAgent(@PathVariable("stationId")stationId: UUID, @RequestBody agent: AgentUpdate): Mono<Agent> = stationService.registerAgent(stationId, agent)

    @DeleteMapping("/{stationId}/agent/{agentId}")
    fun deleteAgent(@PathVariable("stationId")stationId: UUID, @PathVariable("agentId") agentId:UUID): Mono<Void> = stationService.deleteAgent(stationId, agentId)

    @GetMapping("/{stationId}/agent/{agentId}/generate-secret")
    fun generateClientSecret(@PathVariable("stationId")stationId: UUID, @PathVariable("agentId") agentId:UUID): Mono<Agent> = stationService.generateAgentSecret(stationId, agentId)
}