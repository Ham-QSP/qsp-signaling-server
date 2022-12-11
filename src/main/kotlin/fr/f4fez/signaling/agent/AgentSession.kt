package fr.f4fez.signaling.agent

import fr.f4fez.signaling.client.ClientSignalCommand
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketSession

class AgentSession(session: WebSocketSession) {

    val agentClientDescription: AgentClientDescription? = null
    private val logger = KotlinLogging.logger {}
    val agentSessionSocket: AgentSessionSocket

    init {
        this.agentSessionSocket = AgentSessionSocket(session, this)
    }

    fun setAgentHello(clientDescription: AgentClientDescription) {

    }

    fun signalClient(clientSignalCommand: ClientSignalCommand) {
        logger.debug { "[${this.agentClientDescription?.agentName}] Generate client signal message" }
        agentSessionSocket.clientSignal(clientSignalCommand)
    }

}