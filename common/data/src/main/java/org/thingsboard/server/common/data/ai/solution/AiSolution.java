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
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * A persisted AI Solution Creator session: the original prompt, the (possibly refined) architecture
 * spec, its installation status and the entities created on install (so it can be uninstalled).
 * <p>
 * Deliberately a tenant-scoped record rather than a first-class {@code EntityType}, to avoid the
 * platform-wide {@code EntityType}/protobuf/edge registration that a new entity type would require.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A saved AI Solution Creator session")
public class AiSolution implements Serializable {

    @Schema(description = "Solution id (assigned on save)")
    private UUID id;

    @Schema(description = "Creation timestamp (epoch millis)")
    private long createdTime;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Id of the owning tenant")
    private TenantId tenantId;

    @Schema(description = "Record version, incremented on each update")
    private Long version;

    @NoXss
    @Length(min = 1, max = 255)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Solution name")
    private String name;

    @Schema(description = "Original natural-language prompt that started the session")
    private String originalPrompt;

    @Schema(description = "Current installation status")
    private AiSolutionStatus status;

    @Schema(description = "The generated (and refined) architecture specification")
    private AiSolutionSpec spec;

    @Schema(description = "Entities created on install, for later uninstall")
    private List<InstalledEntity> installedEntities;

}
