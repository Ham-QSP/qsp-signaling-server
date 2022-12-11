package fr.f4fez.signaling.agent

data class AgentClientDescription (
    val agentType: String,
    val version: String,
    val protocolMajorVersion: Int,
    val protocolMinorVersion: Int,
    val agentName: String
)
