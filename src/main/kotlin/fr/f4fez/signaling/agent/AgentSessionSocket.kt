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

package fr.f4fez.signaling.agent

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.f4fez.signaling.ServerDescription
import fr.f4fez.signaling.client.AgentDisconnectedException
import fr.f4fez.signaling.client.SessionNotFoundException
import mu.KotlinLogging
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class AgentSessionSocket private constructor(
    private val agentSession: AgentSession,
    private val onHandshakeDone: Consumer<AgentSession>,
    private val onConnectionEnd: Consumer<AgentSession>
) {
    private val logger = KotlinLogging.logger {}
    private val serverDescription = ServerDescription()
    private val objectMapper: ObjectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private lateinit var agentSessionSocketEmitter: AgentSessionSocketEmitter
    lateinit var webSocketPublisher: Mono<Void>
        private set

    private var handshakeDoneFlag = false
    private var exchangeIdCounter = AtomicInteger(1)
    private var sessionExchangeCallbacks: MutableMap<Int, AgentSessionExchangeCallback> =
        Collections.synchronizedMap(mutableMapOf())

    constructor(
        session: WebSocketSession,
        agentSession: AgentSession,
        onHandshakeDone: Consumer<AgentSession>,
        onConnectionEnd: Consumer<AgentSession>
    ) : this(
        agentSession, onHandshakeDone, onConnectionEnd
    ) {
        val input = session.receive().doOnNext { processMessage(it.payloadAsText) }.then()

        val helloFlux = Flux.from(Mono.just(ServerHelloMessage(serverDescription)))
        val connectionFlux = Flux.create {
            agentSessionSocketEmitter = AgentSessionSocketEmitter(it)
        }

        val source = Flux.concat(helloFlux, connectionFlux)
            .doOnError {
                logger.info { "{${agentSession.sessionId}} Session received error: ${it.message}" }
                processConnectionEnd()
            }.doAfterTerminate {
                logger.info { "{${agentSession.sessionId}} Session terminated" }
                processConnectionEnd()
            }.doOnComplete {
                logger.info { "{${agentSession.sessionId}} Session completed" }
                processConnectionEnd()
            }.doOnCancel {
                logger.info { "{${agentSession.sessionId}} Session canceled" }
                processConnectionEnd()
            }
        val output = session.send(source.map { objectMapper.writeValueAsString(it) }.map(session::textMessage))

        this.webSocketPublisher = Mono.zip(input, output).then()
    }

    fun clientSignal(sdp: String): Mono<ClientInitResponsePayload> {
        val exchangeId = exchangeIdCounter.getAndIncrement()
        val mono = Mono.create<AgentSocketMessage> {
            sessionExchangeCallbacks[exchangeId] = AgentSessionExchangeCallback(it) //TODO add expiration
        }.map { it.data as ClientInitResponsePayload }
        logger.trace { "{${agentSession.sessionId}} Register signal exchange callback for exchangeId: $exchangeId" }
        sendExchange(ClientInitMessage(ClientInitPayload(sdp), exchangeId))

        return mono
    }

    private fun sendExchange(agentSocketMessage: AgentSocketMessage) {
        if (!handshakeDoneFlag) throw SessionNotFoundException("Handshake not established")
        agentSessionSocketEmitter.sendExchange(agentSocketMessage)
    }

    private fun processMessage(message: String) {
        try {
            processDeserializedMessage(
                objectMapper.readValue(
                    message, AgentSocketMessage::class.java
                )
            )
            logger.trace { "Valid socket message received: $message" }

        } catch (e: JsonProcessingException) {
            sendErrorAndClose(101, "Failed to decode message ${e.message}")
        }

    }

    private fun processDeserializedMessage(message: AgentSocketMessage) {
        when (message) {
            is AgentHelloMessage -> processAgentHello(message)
            is ClientInitResponseMessage -> processAgentInitResponse(message)
            is GenericErrorResponse -> processInvalidMessageCommand(message.command)
            is ClientInitMessage -> processInvalidMessageCommand(message.command)
            is ServerHelloMessage -> processInvalidMessageCommand(message.command)
        }
    }

    private fun processAgentHello(message: AgentHelloMessage) {
        if (handshakeDoneFlag) {
            sendErrorAndClose(104, "Received Agent hello more than one time. ${message.data}", message.exchangeId)
        } else {
            agentSession.agentClientDescription = message.data
            handshakeDoneFlag = true
            onHandshakeDone.accept(agentSession)
            logger.info { "{${agentSession.sessionId}} Agent registered ${message.data}" }
        }
    }

    private fun processAgentInitResponse(message: ClientInitResponseMessage) {
        val callback = sessionExchangeCallbacks.remove(message.exchangeId)
        logger.trace { "{${agentSession.sessionId}} Call callback for exchangeId: ${message.exchangeId}" }
        callback?.responseReceived(message) ?: sendErrorAndClose(
            103, "Can't find client init SDP request", message.exchangeId
        )
    }

    private fun processInvalidMessageCommand(commandName: String) {
        sendErrorAndClose(102, "Server received invalid command name: $commandName not allowed.")
    }

    private fun sendErrorAndClose(code: Int, message: String, exchangeId: Int = 0) {
        logger.debug { "[${this.agentSession.agentClientDescription?.agentName}] $message" }
        sendExchange(GenericErrorResponse(code, message, exchangeId))
        agentSessionSocketEmitter.close()
        processConnectionEnd()
    }

    private fun processConnectionEnd() {
        onConnectionEnd.accept(agentSession)
        sessionExchangeCallbacks.forEach { it.value.error(AgentDisconnectedException("Remote agent disconnected")) }
    }

    private inner class AgentSessionSocketEmitter(val sink: FluxSink<AgentSocketMessage>) {
        fun sendExchange(agentSocketMessage: AgentSocketMessage) {
            sink.next(agentSocketMessage)
        }

        fun close() {
            sink.complete()
        }
    }

    private inner class AgentSessionExchangeCallback(
        val sink: MonoSink<AgentSocketMessage>
    ) {
        fun responseReceived(agentSocketMessage: AgentSocketMessage) {
            sink.success(agentSocketMessage)
        }

        fun error(e: Throwable) {
            sink.error(e)
        }
    }

}