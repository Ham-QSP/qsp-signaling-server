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
package fr.f4fez.signaling.security

import fr.f4fez.signaling.management.dal.StationGrantRepository
import fr.f4fez.signaling.management.dal.StationGrantRole
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*

/**
 * Backs the `@PreAuthorize("@stationAccess...")` expressions on station-scoped controller methods.
 * MEMBER is satisfied by any grant (read access); OWNER requires the OWNER role specifically (write access).
 */
@Component("stationAccess")
class StationAccess(
    private val stationGrantRepository: StationGrantRepository,
) {

    fun isMember(stationId: UUID): Mono<Boolean> =
        currentUserSubject()
            .flatMap { subject -> stationGrantRepository.existsByStationIdAndSubject(stationId, subject) }
            .defaultIfEmpty(false)

    fun isOwner(stationId: UUID): Mono<Boolean> =
        currentUserSubject()
            .flatMap { subject ->
                stationGrantRepository.findByStationIdAndSubject(stationId, subject)
                    .map { it.role == StationGrantRole.OWNER }
            }
            .defaultIfEmpty(false)
}
