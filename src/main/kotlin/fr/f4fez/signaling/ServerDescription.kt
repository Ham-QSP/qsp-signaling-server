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

package fr.f4fez.signaling

data class ServerDescription(
    val serverName: String = "Dev server",
    val serverType: String = "QSP Simple Signal Server",
    val version: String = "0.1.0",
    val protocolMajorVersion: Int = 0,
    val protocolMinorVersion: Int = 1
)