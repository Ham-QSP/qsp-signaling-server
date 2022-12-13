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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.f4fez.signaling.ServerDescription
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
    private val agentSession: AgentSession, private val handshakeDone: Consumer<AgentSession>
) {
    private val logger = KotlinLogging.logger {}
    private val serverDescription = ServerDescription()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var agentSessionSocketEmitter: AgentSessionSocketEmitter
    lateinit var webSocketPublisher: Mono<Void>
        private set

    private var handshakeDoneFlag = false
    private var exchangeIdCounter = AtomicInteger()
    private var sessionExchangeCallbacks: MutableMap<Int, AgentSessionExchangeCallback> =
        Collections.synchronizedMap(mutableMapOf())

    constructor(session: WebSocketSession, agentSession: AgentSession, handshakeDone: Consumer<AgentSession>) : this(
        agentSession, handshakeDone
    ) {
        val input = session.receive().doOnNext {
                processMessage(it.payloadAsText)
            }.then()

        val helloFlux = Flux.from(Mono.just(ServerHelloMessage(serverDescription)))
        val connectionFlux = Flux.create {
            agentSessionSocketEmitter = AgentSessionSocketEmitter(it)
        }

        val source = Flux.concat(helloFlux, connectionFlux)
        val output = session.send(source.map { objectMapper.writeValueAsString(it) }.map(session::textMessage))

        this.webSocketPublisher = Mono.zip(input, output).then()
    }

    fun clientSignal(sdp: String): Mono<String> {
        val exchangeId = exchangeIdCounter.getAndIncrement()
        val mono = Mono.create<AgentSocketMessage> {
            sessionExchangeCallbacks[exchangeId] = AgentSessionExchangeCallback(it) //FIXME add expiration
        }.map { it.data as String }
        sendExchange(ClientInitMessage(ClientInitPayload(sdp), exchangeId))

        return mono
    }

    private fun sendExchange(agentSocketMessage: AgentSocketMessage) {
        //FIXME check for handshake done
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

        } catch (e: Exception) {
            logger.info("Failed to decode message $message", e)
            sendExchange(GenericErrorResponse(101, "Failed to decode message ${e.message}"))
            agentSessionSocketEmitter.close()
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
            handshakeDone.accept(agentSession)
        }
    }

    private fun processAgentInitResponse(message: ClientInitResponseMessage) {
        val callback = sessionExchangeCallbacks[message.exchangeId]

        callback?.responseReceived(message) ?: sendErrorAndClose(
            103,
            "Can't find client init SDP request",
            message.exchangeId
        )
    }

    private fun processInvalidMessageCommand(commandName: String) {
        sendErrorAndClose(102, "Server received invalid command name: $commandName not allowed.")
    }

    private fun sendErrorAndClose(code: Int, message: String, exchangeId: Int = 0) {
        logger.debug { "[${this.agentSession.agentClientDescription?.agentName}] $message" }
        sendExchange(GenericErrorResponse(code, message, exchangeId))
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

    private inner class AgentSessionExchangeCallback(val sink: MonoSink<AgentSocketMessage>) {
        fun responseReceived(agentSocketMessage: AgentSocketMessage) {
            sink.success(agentSocketMessage)
        }
    }

}