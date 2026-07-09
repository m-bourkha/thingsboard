/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.ai.solution;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.io.Serializable;
import java.util.UUID;

/**
 * A reference to an entity created during installation, recorded on the {@code AiSolution} so the
 * solution can be cleanly uninstalled later (entities are deleted in reverse creation order).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Reference to an entity created while installing an AI solution")
public record InstalledEntity(
        @Schema(description = "Type of the created entity")
        EntityType entityType,

        @Schema(description = "Id of the created entity")
        UUID id,

        @Schema(description = "Display name of the created entity")
        String name
) implements Serializable {
}
