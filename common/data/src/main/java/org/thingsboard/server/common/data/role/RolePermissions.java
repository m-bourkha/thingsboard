/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Typed representation of the permissions stored for a {@link Role}, persisted as a jsonb column.
 * <p>
 * Leaf values are kept as {@link String} (the names of the security {@code Operation} / {@code Resource}
 * enums, which live in the {@code application} module that {@code common-data} cannot depend on).
 */
@Data
public class RolePermissions {

    @Schema(description = "List of operations granted by a GROUP role")
    private List<String> operations;

    @Schema(description = "Per-resource allowed/denied operations granted by a GENERIC role")
    private List<GenericPermission> generic;

    @Data
    public static class GenericPermission {

        @Schema(description = "Name of the resource the permission applies to")
        private String resource;

        @Schema(description = "Operations explicitly allowed on the resource")
        private List<String> allowed;

        @Schema(description = "Operations explicitly denied on the resource")
        private List<String> denied;
    }
}
