package fr.f4fez.signaling.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.f4fez.signaling.ServerDescription
import fr.f4fez.signaling.client.ClientSignalCommand
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono

class AgentSessionSocket private constructor(private val agentSession: AgentSession) {
    private val logger = KotlinLogging.logger {}
    private val serverDescription = ServerDescription()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var agentSessionSocketEmitter: AgentSessionSocketEmitter
    lateinit var webSocketPublisher: Mono<Void>
        private set

    private var handshakeDone = false

    constructor(session: WebSocketSession, agentSession: AgentSession) : this(agentSession) {
        val input = session.receive()
            .doOnNext {
                processMessage(it.payloadAsText)
            }
            .then()

        val helloFlux = Flux.from(Mono.just(ServerHelloMessage(serverDescription)))
        val connectionFlux = Flux.create {
            agentSessionSocketEmitter = AgentSessionSocketEmitter(it)
        }

        val source = Flux.concat(helloFlux, connectionFlux)
        val output = session.send(source.map { objectMapper.writeValueAsString(it) }.map(session::textMessage))

        this.webSocketPublisher = Mono.zip(input, output).then()
    }


    fun clientSignal(clientSignalCommand: ClientSignalCommand) {
        sendExchange(ClientInitMessage(clientSignalCommand.clientSdp))
    }

    private fun sendExchange(agentSocketMessage: AgentSocketMessage) {
        //FIXME check for handshake done
        agentSessionSocketEmitter.sendExchange(agentSocketMessage)
    }

    private fun processMessage(message: String) {
        try {
            processDeserializedMessage(
                objectMapper.readValue(
                    message,
                    AgentSocketMessage::class.java
                )
            )
            logger.trace { "Valid socket message received: $message" }

        } catch (e: Exception) {
            logger.info("Failed to decode message $message", e)
            sendExchange(GenericErrorResponse(101, "Failed to decode message ${e.message}"))
            agentSessionSocketEmitter.close()
        }

    }

    private fun processDeserializedMessage(message: AgentSocketMessage) {
        when (message) {
            is AgentHelloMessage -> processAgentHello(message)
            is ClientInitResponseMessage -> TODO()
            is GenericErrorResponse -> processInvalidMessageCommand(message.command)
            is ClientInitMessage -> processInvalidMessageCommand(message.command)
            is ServerHelloMessage -> processInvalidMessageCommand(message.command)
        }
    }

    private fun processAgentHello(message: AgentHelloMessage) {
        if (handshakeDone) {
            logger.info { "[${this.agentSession.agentClientDescription?.agentName}] Received Agent hello more than one time. ${message.data}" }
            agentSessionSocketEmitter.close()
        } else {
            agentSession.setAgentHello(message.data)
            handshakeDone = true
        }
    }

    private fun processInvalidMessageCommand(commandName: String) {
        logger.info { "[${this.agentSession.agentClientDescription?.agentName}] Server received invalid command name: $commandName not allowed." }
        sendExchange(GenericErrorResponse(102, "Server received invalid command name: $commandName not allowed."))
        agentSessionSocketEmitter.close()
    }

    private inner class AgentSessionSocketEmitter(val sink: FluxSink<AgentSocketMessage>) {
        fun sendExchange(agentSocketMessage: AgentSocketMessage) {
            sink.next(agentSocketMessage)
        }

        fun close() {
            sink.complete()
        }
    }

}