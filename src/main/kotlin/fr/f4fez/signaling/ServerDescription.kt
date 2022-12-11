package fr.f4fez.signaling

data class ServerDescription(
    val serverType: String = "F4FEZ Simple Signal Server",
    val version: String = "0.1.0",
    val protocolMajorVersion: Int = 0,
    val protocolMinorVersion: Int = 1,
    val serverName: String = "Dev"
)