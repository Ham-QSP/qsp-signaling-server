package fr.f4fez.signaling.client

data class ClientSignalCommand(
    var clientSdp: String
)

data class ClientSignalResponse(
    var serverSdp: String
)