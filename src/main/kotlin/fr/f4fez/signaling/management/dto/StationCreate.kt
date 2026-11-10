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

import fr.f4fez.signaling.error.InvalidRequest
import fr.f4fez.signaling.management.dal.StationEntity
import fr.f4fez.signaling.management.dal.StationGrantRole

private const val MAX_QRZ_LENGTH = 20
private const val MAX_SUBJECT_LENGTH = 100
private val QRZ_FORMAT = Regex("^[A-Z0-9_]+$")

/** Throws [InvalidRequest] when [mainQRZ] is not a valid station name. */
fun validateMainQRZ(mainQRZ: String) {
    if (mainQRZ.length > MAX_QRZ_LENGTH || !QRZ_FORMAT.matches(mainQRZ)) {
        throw InvalidRequest("mainQRZ must be between 1 and $MAX_QRZ_LENGTH characters matching ${QRZ_FORMAT.pattern}")
    }
}

class StationCreate(
    var mainQRZ: String,
    var description: String,
    var grants: List<StationGrantUpdate>,
) {
    /** Throws [InvalidRequest] when the station or its initial grants are not acceptable. */
    fun validate() {
        validateMainQRZ(mainQRZ)
        if (grants.none { it.role == StationGrantRole.OWNER }) {
            throw InvalidRequest("At least one grant with the OWNER role is required")
        }
        if (grants.any { it.subject.isBlank() || it.subject.length > MAX_SUBJECT_LENGTH }) {
            throw InvalidRequest("Grant subject must be between 1 and $MAX_SUBJECT_LENGTH characters")
        }
        if (grants.map { it.subject }.toSet().size != grants.size) {
            throw InvalidRequest("Grant subjects must be unique")
        }
    }
}

fun StationCreate.toEntity() = StationEntity(mainQRZ, description)
