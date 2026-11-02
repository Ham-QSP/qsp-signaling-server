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

import fr.f4fez.signaling.security.currentUserSubject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = ["http://localhost:3000"])
class AgentController(val agentService: AgentService,
                      val agentSessionService: AgentSessionService) {

    @PostMapping("client-signal")
    fun signal(@RequestBody clientSignalCommand: ClientSignalCommand) =
        currentUserSubject().flatMap { subject -> agentService.signalClient(clientSignalCommand, subject) }

    @GetMapping("online")
    fun listAgents() = currentUserSubject().flatMapMany { subject -> agentSessionService.getSessions(subject) }

    @GetMapping
    fun getAgents() = currentUserSubject().flatMapMany { subject -> agentService.getAgentsForSubject(subject) }
}