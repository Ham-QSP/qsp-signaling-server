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
import fr.f4fez.signaling.management.dal.AgentRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
class AgentSessionSocketService(
    private val agentService: AgentService,
    private val agentRepository: AgentRepository,
) {
    private val logger = KotlinLogging.logger {}
    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun startSession(
        session: WebSocketSession,
        serverDescription: ServerDescription,
    ): Mono<Void> {
        val agentSession = AgentSession(
            { agentService.registerSession(it) },
            { agentService.unregisterSession(it.sessionId) },
        )

        val input = messageProcessing(agentSession, session.receive())

        val helloFlux = Flux.from(Mono.just(ServerHelloMessage(serverDescription)))
        val connectionFlux = Flux.create {
            agentSession.agentSessionSocketEmitter = agentSession.AgentSessionSocketEmitter(it)
        }

        val source = connectionFlux.startWith(helloFlux)
            .doOnError {
                logger.info { "{${agentSession.sessionId}} Session received error: ${it.message}" }
                processConnectionEnd(agentSession)
            }.doAfterTerminate {
                logger.info { "{${agentSession.sessionId}} Session terminated" }
                processConnectionEnd(agentSession)
            }.doOnComplete {
                logger.info { "{${agentSession.sessionId}} Session completed" }
                processConnectionEnd(agentSession)
            }.doOnCancel {
                logger.info { "{${agentSession.sessionId}} Session canceled" }
                processConnectionEnd(agentSession)
            }
        val output = session.send(source.map { objectMapper.writeValueAsString(it) }.map(session::textMessage))

        return Mono.zip(input, output).then()
    }

    fun clientSignal(agentSession: AgentSession, sdp: String): Mono<ClientInitResponsePayload> {
        logger.debug { "{${agentSession.sessionId}} Send client init" }
        val exchangeId = agentSession.exchangeIdCounter.getAndIncrement()
        val mono = Mono.create<AgentSocketMessage> {
            agentSession.sessionExchangeCallbacks[exchangeId] =
                agentSession.AgentSessionExchangeCallback(it) //TODO add expiration
        }.map { it.data as ClientInitResponsePayload }
        logger.trace { "{${agentSession.sessionId}} Register signal exchange callback for exchangeId: $exchangeId" }
        sendExchange(agentSession, ClientInitMessage(ClientInitPayload(sdp), exchangeId))

        return mono
    }

    private fun sendExchange(agentSession: AgentSession, agentSocketMessage: AgentSocketMessage) {
        if (!agentSession.handshakeDoneFlag) throw SessionNotFoundException("Handshake not established")
        agentSession.agentSessionSocketEmitter?.sendExchange(agentSocketMessage)
    }

    private fun messageProcessing(
        agentSession: AgentSession,
        webSocketMessageFlux: Flux<WebSocketMessage>
    ): Mono<Void> {
        return webSocketMessageFlux.mapNotNull { deserializeMessage(agentSession, it) }
            .flatMap { message -> checkAuthentication(message!!) }
            .doOnNext { it?.let { processDeserializedMessage(agentSession, it) } }
            .then()
    }

    private fun checkAuthentication(message: AgentSocketMessage): Mono<AgentSocketMessage> {
        val flux = if (message is AgentHelloMessage) {
            val agentId = UUID.fromString(message.data.agentId)
            val ret: Mono<AgentSocketMessage> = agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(NullPointerException("Agent not found")))
                .doOnNext { }
                .map { _ -> message }
            ret
        } else {
            Mono.just(message)
        }
        return flux
    }

    private fun deserializeMessage(agentSession: AgentSession, message: WebSocketMessage): AgentSocketMessage? {
        try {
            return objectMapper.readValue(message.payloadAsText, AgentSocketMessage::class.java)
        } catch (e: JsonProcessingException) {
            sendErrorAndClose(agentSession, 101, "Failed to decode message ${e.message}")
            return null
        }
    }

    private fun processDeserializedMessage(agentSession: AgentSession, message: AgentSocketMessage) {
        when (message) {
            is AgentHelloMessage -> processAgentHello(agentSession, message)
            is ClientInitResponseMessage -> processAgentInitResponse(agentSession, message)
            is GenericErrorResponse -> processInvalidMessageCommand(agentSession, message.command)
            is ClientInitMessage -> processInvalidMessageCommand(agentSession, message.command)
            is ServerHelloMessage -> processInvalidMessageCommand(agentSession, message.command)
        }
    }

    private fun processAgentHello(agentSession: AgentSession, message: AgentHelloMessage) {
        if (agentSession.handshakeDoneFlag) {
            sendErrorAndClose(
                agentSession,
                104,
                "Received Agent hello more than one time. ${message.data}",
                message.exchangeId
            )
        } else {
            agentSession.agentClientDescription = message.data
            agentSession.handshakeDoneFlag = true
            agentSession.onHandshakeDone.accept(agentSession)
            logger.info { "{${agentSession.sessionId}} Agent registered ${message.data}" }
        }
    }

    private fun processAgentInitResponse(agentSession: AgentSession, message: ClientInitResponseMessage) {
        val callback = agentSession.sessionExchangeCallbacks.remove(message.exchangeId)
        logger.trace { "{${agentSession.sessionId}} Call callback for exchangeId: ${message.exchangeId}" }
        callback?.responseReceived(message) ?: sendErrorAndClose(
            agentSession, 103, "Can't find client init SDP request", message.exchangeId
        )
    }

    private fun processInvalidMessageCommand(agentSession: AgentSession, commandName: String) {
        sendErrorAndClose(agentSession, 102, "Server received invalid command name: $commandName not allowed.")
    }

    private fun sendErrorAndClose(agentSession: AgentSession, code: Int, message: String, exchangeId: Int = 0) {
        logger.debug { "[${agentSession.agentClientDescription?.agentName}] $message" }
        sendExchange(agentSession, GenericErrorResponse(code, message, exchangeId))
        agentSession.agentSessionSocketEmitter?.close()
        processConnectionEnd(agentSession)
    }

    private fun processConnectionEnd(agentSession: AgentSession) {
        agentSession.onConnectionEnd.accept(agentSession)
        agentSession.sessionExchangeCallbacks.forEach { it.value.error(AgentDisconnectedException("Remote agent disconnected")) }
    }

}