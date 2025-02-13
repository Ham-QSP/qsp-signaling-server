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

package fr.f4fez.signaling.client

import fr.f4fez.signaling.agent.AgentService
import fr.f4fez.signaling.agent.AgentSessionService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = ["http://localhost:5173"])
class ClientController(val agentService: AgentService,
    val agentSessionService: AgentSessionService) {

    @PostMapping("signal")
    fun signal(@RequestBody clientSignalCommand: ClientSignalCommand) =
        agentService.signalClient(clientSignalCommand)

    @GetMapping("agents")
    fun listAgents() = agentSessionService.getSessions()

}