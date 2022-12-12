package fr.f4fez.signaling.client

import fr.f4fez.signaling.agent.AgentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/client")
class ClientController(val agentService: AgentService) {

    @PostMapping("signal")
    fun signal(@RequestBody clientSignalCommand: ClientSignalCommand) =
        agentService.signalClient(clientSignalCommand)

    @GetMapping("agents")
    fun listAgents() = agentService.getSessions()

}