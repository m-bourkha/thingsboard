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
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroupPermission extends BaseData<GroupPermissionId>
        implements HasTenantId, HasVersion {

    private static final long serialVersionUID = -1022736540062754116L;

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with User Group Id")
    private EntityGroupId userGroupId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with Role Id")
    private RoleId roleId;

    @Schema(description = "JSON object with Entity Group Id (null means all groups of entityGroupType)")
    private EntityGroupId entityGroupId;

    @Schema(description = "Entity type that the permission applies to")
    private EntityType entityGroupType;

    @Schema(description = "True if this permission applies to public entity groups")
    private boolean publicPermission;

    @Getter @Setter
    private Long version;

    @Getter @Setter
    private GroupPermissionId externalId;

    public GroupPermission() {
        super();
    }

    public GroupPermission(GroupPermissionId id) {
        super(id);
    }

    public GroupPermission(GroupPermission permission) {
        super(permission);
        this.tenantId = permission.getTenantId();
        this.userGroupId = permission.getUserGroupId();
        this.roleId = permission.getRoleId();
        this.entityGroupId = permission.getEntityGroupId();
        this.entityGroupType = permission.getEntityGroupType();
        this.publicPermission = permission.isPublicPermission();
        this.version = permission.getVersion();
        this.externalId = permission.getExternalId();
    }



    public EntityGroupId getUserGroupId() {
        return userGroupId;
    }

    public void setUserGroupId(EntityGroupId userGroupId) {
        this.userGroupId = userGroupId;
    }

    public RoleId getRoleId() {
        return roleId;
    }

    public void setRoleId(RoleId roleId) {
        this.roleId = roleId;
    }

    public EntityGroupId getEntityGroupId() {
        return entityGroupId;
    }

    public void setEntityGroupId(EntityGroupId entityGroupId) {
        this.entityGroupId = entityGroupId;
    }

    public EntityType getEntityGroupType() {
        return entityGroupType;
    }

    public void setEntityGroupType(EntityType entityGroupType) {
        this.entityGroupType = entityGroupType;
    }

    public boolean isPublicPermission() {
        return publicPermission;
    }

    public void setPublicPermission(boolean publicPermission) {
        this.publicPermission = publicPermission;
    }
}
