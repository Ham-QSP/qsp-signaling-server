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
import fr.f4fez.signaling.management.dto.StationGrant
import fr.f4fez.signaling.management.dto.StationGrantUpdate
import fr.f4fez.signaling.management.dto.StationCreate
import fr.f4fez.signaling.management.dto.StationUpdate
import fr.f4fez.signaling.management.dto.Station
import fr.f4fez.signaling.security.currentUserSubject
import org.springframework.security.core.parameters.P
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/station")
class StationController(val stationService: StationService) {

    @GetMapping
    fun getStations(): Flux<Station> = currentUserSubject().flatMapMany { subject -> stationService.getStations(subject) }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_AUTH')")
    @PostMapping
    fun createStation(@RequestBody station: StationCreate): Mono<Station> = stationService.createStation(station)

    @PreAuthorize("@stationAccess.isMember(#id)")
    @GetMapping("/{id}")
    fun getStation(@P("id") @PathVariable("id") id: UUID): Mono<Station> = stationService.getStation(id)

    @PreAuthorize("@stationAccess.isOwner(#id)")
    @PutMapping("/{id}")
    fun updateStation(@P("id") @PathVariable("id") id: UUID, @RequestBody station: StationUpdate): Mono<Station> =
        stationService.updateStation(id, station)

    @PreAuthorize("@stationAccess.isOwner(#id)")
    @DeleteMapping("/{id}")
    fun deleteStation(@P("id") @PathVariable("id") id: UUID): Mono<Void> = stationService.deleteStation(id)

    @PreAuthorize("@stationAccess.isMember(#stationId)")
    @GetMapping("/{stationId}/agent")
    fun getAgents(@P("stationId") @PathVariable("stationId")stationId: UUID): Flux<Agent> = stationService.getAgents(stationId)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @PostMapping("/{stationId}/agent")
    fun registerAgent(@P("stationId") @PathVariable("stationId")stationId: UUID, @RequestBody agent: AgentUpdate): Mono<Agent> = stationService.registerAgent(stationId, agent)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @DeleteMapping("/{stationId}/agent/{agentId}")
    fun deleteAgent(@P("stationId") @PathVariable("stationId")stationId: UUID, @PathVariable("agentId") agentId:UUID): Mono<Void> = stationService.deleteAgent(stationId, agentId)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @GetMapping("/{stationId}/agent/{agentId}/generate-secret")
    fun generateClientSecret(@P("stationId") @PathVariable("stationId")stationId: UUID, @PathVariable("agentId") agentId:UUID): Mono<Agent> = stationService.generateAgentSecret(stationId, agentId)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @GetMapping("/{stationId}/grant")
    fun getGrants(@P("stationId") @PathVariable("stationId") stationId: UUID): Flux<StationGrant> = stationService.getGrants(stationId)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @PostMapping("/{stationId}/grant")
    fun createGrant(@P("stationId") @PathVariable("stationId") stationId: UUID, @RequestBody grant: StationGrantUpdate): Mono<StationGrant> =
        stationService.createGrant(stationId, grant)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @PutMapping("/{stationId}/grant/{grantId}")
    fun updateGrant(
        @P("stationId") @PathVariable("stationId") stationId: UUID,
        @PathVariable("grantId") grantId: UUID,
        @RequestBody grant: StationGrantUpdate,
    ): Mono<StationGrant> = stationService.updateGrant(stationId, grantId, grant)

    @PreAuthorize("@stationAccess.isOwner(#stationId)")
    @DeleteMapping("/{stationId}/grant/{grantId}")
    fun deleteGrant(@P("stationId") @PathVariable("stationId") stationId: UUID, @PathVariable("grantId") grantId: UUID): Mono<Void> =
        stationService.deleteGrant(stationId, grantId)
}