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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * Identity and access management section of an {@link AiSolutionSpec}. Mirrors the ThingsBoard
 * RBAC model: {@link RoleSpec roles}, {@link EntityGroupSpec entity groups}, {@link UserSpec users}
 * and {@link GroupPermissionSpec permissions} that bind a user group to a role over an entity group.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IamSpec(
        @Schema(description = "Roles granted to users")
        List<RoleSpec> roles,

        @Schema(description = "Entity groups (device/asset/customer/user) to create")
        List<EntityGroupSpec> entityGroups,

        @Schema(description = "Users to create")
        List<UserSpec> users,

        @Schema(description = "Permissions binding a user group to a role over an entity group")
        List<GroupPermissionSpec> permissions
) implements Serializable {

    public List<RoleSpec> rolesOrEmpty() {
        return roles != null ? roles : List.of();
    }

    public List<EntityGroupSpec> entityGroupsOrEmpty() {
        return entityGroups != null ? entityGroups : List.of();
    }

    public List<UserSpec> usersOrEmpty() {
        return users != null ? users : List.of();
    }

    public List<GroupPermissionSpec> permissionsOrEmpty() {
        return permissions != null ? permissions : List.of();
    }

    /**
     * A role. GROUP roles carry a flat {@code operations} list (applied to an entity group via a
     * permission); GENERIC roles carry per-resource allowed/denied operation lists (tenant-wide).
     * Operation and resource values are names of the security {@code Operation} / {@code Resource}
     * enums and are validated by the installer.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoleSpec(
            @Schema(description = "Role name", example = "Parking Technician")
            String name,

            @Schema(description = "GENERIC (tenant-wide, per-resource) or GROUP (operations on a group)")
            RoleKind type,

            @Schema(description = "Description of the role")
            String description,

            @Schema(description = "Operations for a GROUP role, e.g. [CREATE, READ, WRITE, DELETE]")
            List<String> operations,

            @Schema(description = "Per-resource allowed/denied operations for a GENERIC role")
            List<GenericPermissionSpec> generic
    ) implements Serializable {

        public enum RoleKind {
            GENERIC, GROUP
        }

        public List<String> operationsOrEmpty() {
            return operations != null ? operations : List.of();
        }

        public List<GenericPermissionSpec> genericOrEmpty() {
            return generic != null ? generic : List.of();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenericPermissionSpec(
            @Schema(description = "Resource name (a value of the security Resource enum)", example = "DEVICE")
            String resource,

            @Schema(description = "Operations explicitly allowed on the resource")
            List<String> allowed,

            @Schema(description = "Operations explicitly denied on the resource")
            List<String> denied
    ) implements Serializable {

        public List<String> allowedOrEmpty() {
            return allowed != null ? allowed : List.of();
        }

        public List<String> deniedOrEmpty() {
            return denied != null ? denied : List.of();
        }
    }

    /**
     * An entity group of a given entity type, owned by the tenant or a named customer.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntityGroupSpec(
            @Schema(description = "Group name", example = "Parking Technicians")
            String name,

            @Schema(description = "Grouped entity type: DEVICE, ASSET, CUSTOMER or USER")
            String entityType,

            @Schema(description = "Owner: 'TENANT' or the name of a customer defined in this solution")
            String owner
    ) implements Serializable {
    }

    /**
     * A user to create and place into a user entity group.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserSpec(
            @Schema(description = "Login email", example = "technician@parking.io")
            String email,

            @Schema(description = "First name")
            String firstName,

            @Schema(description = "Last name")
            String lastName,

            @Schema(description = "TENANT_ADMIN or CUSTOMER_USER")
            String authority,

            @Schema(description = "Name of the owning customer (required for CUSTOMER_USER)")
            String customer,

            @Schema(description = "Name of the user entity group to place the user into")
            String userGroup
    ) implements Serializable {
    }

    /**
     * Grants a user group a role, optionally scoped to a specific entity group and type.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupPermissionSpec(
            @Schema(description = "Name of the user entity group being granted the role")
            String userGroup,

            @Schema(description = "Name of the role to grant")
            String role,

            @Schema(description = "Name of the entity group the role applies to (GROUP roles only)")
            String entityGroup,

            @Schema(description = "Entity type of the target group (GROUP roles only)")
            String entityGroupType
    ) implements Serializable {
    }

}
