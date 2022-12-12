package fr.f4fez.signaling.agent

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fr.f4fez.signaling.ServerDescription

const val MESSAGE_SERVER_HELLO = "SERVER_HELLO"
const val MESSAGE_AGENT_HELLO = "AGENT_HELLO"
const val MESSAGE_CLIENT_INIT = "CLIENT_INIT"
const val MESSAGE_INIT_RESPONSE = "INIT_RESPONSE"
const val MESSAGE_ERROR = "ERROR"

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "command"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = AgentHelloMessage::class, name = MESSAGE_AGENT_HELLO),
    JsonSubTypes.Type(value = ClientInitResponseMessage::class, name = MESSAGE_INIT_RESPONSE),
    JsonSubTypes.Type(value = GenericErrorResponse::class, name = MESSAGE_ERROR)
)
sealed class AgentSocketMessage(
    val command: String,
    val exchangeId: Int = 0,
    open val data: Any? = null,
) {
    override fun toString(): String {
        return "${this.javaClass.name}($data)"
    }
}

interface AgentSocketMessageResponse {
    val errorCode: Int
    val errorMessage: String?
}

class GenericErrorResponse(
    override val errorCode: Int,
    override val errorMessage: String?,
    exchangeId: Int = 0
) :
    AgentSocketMessage(MESSAGE_ERROR, exchangeId), AgentSocketMessageResponse

class ServerHelloMessage(override val data: ServerDescription) : AgentSocketMessage(MESSAGE_SERVER_HELLO)
class AgentHelloMessage(override val data: AgentClientDescription) : AgentSocketMessage(MESSAGE_AGENT_HELLO)

class ClientInitMessage(override val data: ClientInitPayload, exchangeId: Int) :
    AgentSocketMessage(MESSAGE_CLIENT_INIT, exchangeId)

class ClientInitResponseMessage(override val data: ClientInitResponsePayload, exchangeId: Int) :
    AgentSocketMessage(MESSAGE_INIT_RESPONSE, exchangeId)