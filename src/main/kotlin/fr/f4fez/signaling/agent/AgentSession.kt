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

import reactor.core.publisher.FluxSink
import reactor.core.publisher.MonoSink
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class AgentSession(
    val onHandshakeDone: Consumer<AgentSession>,
    val onConnectionEnd: Consumer<AgentSession>,
) {
    var agentClientDescription: AgentClientDescription? = null
    val sessionId: String = UUID.randomUUID().toString()
    var agentSessionSocketEmitter: AgentSessionSocketEmitter? = null

    var handshakeDoneFlag = false
    var exchangeIdCounter = AtomicInteger(1)
    var sessionExchangeCallbacks: MutableMap<Int, AgentSessionExchangeCallback> =
        Collections.synchronizedMap(mutableMapOf())

    inner class AgentSessionSocketEmitter(val sink: FluxSink<AgentSocketMessage>) {
        fun sendExchange(agentSocketMessage: AgentSocketMessage) {
            sink.next(agentSocketMessage)
        }

        fun close() {
            sink.complete()
        }
    }

    inner class AgentSessionExchangeCallback(
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