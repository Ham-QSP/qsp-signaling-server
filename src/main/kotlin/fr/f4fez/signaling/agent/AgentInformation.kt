package fr.f4fez.signaling.agent

import fr.f4fez.signaling.management.dal.AgentEntity
import java.util.*

data class AgentInformation(
    var stationId: UUID,
    var displayName: String,
    var secret: String?,
    val id: UUID? = null,
)

fun AgentEntity.toAgentInformation() = AgentInformation(stationId, displayName, secret, id)
