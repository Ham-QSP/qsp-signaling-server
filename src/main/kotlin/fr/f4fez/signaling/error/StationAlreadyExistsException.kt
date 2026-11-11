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
package fr.f4fez.signaling.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

/**
 * Answered as 409 with the existing station id in the body (`stationId`), so that callers can grant access to it.
 */
class StationAlreadyExistsException(val mainQRZ: String, val stationId: UUID) :
    RuntimeException("Station $mainQRZ already exists")

@RestControllerAdvice
class StationAlreadyExistsHandler {
    @ExceptionHandler(StationAlreadyExistsException::class)
    fun handle(e: StationAlreadyExistsException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("message" to (e.message ?: ""), "stationId" to e.stationId))
}
