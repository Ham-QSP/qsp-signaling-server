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

package fr.f4fez.signaling.configuration

import fr.f4fez.signaling.server.ServerDescription
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val BEARER_AUTH_SCHEME = "bearerAuth"

@Configuration
class OpenApiConfiguration {

    @Bean
    fun openApi(serverDescription: ServerDescription): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title(serverDescription.serverType)
                    .description(serverDescription.description)
                    .version(serverDescription.version)
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        BEARER_AUTH_SCHEME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(BEARER_AUTH_SCHEME))
}
