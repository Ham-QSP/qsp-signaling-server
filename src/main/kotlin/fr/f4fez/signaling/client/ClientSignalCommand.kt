package fr.f4fez.signaling.client

data class ClientSignalCommand(
    var agentSessionId: String,
    var clientSdp: String
)

data class ClientSignalResponse(
    var serverSdp: String
)