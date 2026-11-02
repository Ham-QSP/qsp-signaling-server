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
import fr.f4fez.signaling.agent.ClientSignalCommand
import fr.f4fez.signaling.security.currentUserSubject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = ["http://localhost:3000"])
@Deprecated(message = "Moved", level = DeprecationLevel.HIDDEN, replaceWith = ReplaceWith("fr.f4fez.signaling.agent.AgentController"))
class ClientController(val agentService: AgentService,
    val agentSessionService: AgentSessionService) {

    @PostMapping("signal")
    fun signal(@RequestBody clientSignalCommand: ClientSignalCommand) =
        currentUserSubject().flatMap { subject -> agentService.signalClient(clientSignalCommand, subject) }

    @GetMapping("agents")
    fun listAgents() = currentUserSubject().flatMapMany { subject -> agentSessionService.getSessions(subject) }

}