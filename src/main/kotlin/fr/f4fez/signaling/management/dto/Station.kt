package fr.f4fez.signaling.management.dto

import fr.f4fez.signaling.error.InvalidRequest
import fr.f4fez.signaling.management.dal.StationEntity
import java.util.*

data class Station (
    val id: UUID,
    val mainQRZ: String,
    val description: String,
)

fun StationEntity.toDTO() = Station(id?: throw InvalidRequest("Id can't be null"), mainQRZ, description)

