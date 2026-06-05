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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.model.BaseVersionedEntity;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "group_permission")
public final class GroupPermissionEntity extends BaseVersionedEntity<GroupPermission> {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_group_id")
    private UUID userGroupId;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "entity_group_id")
    private UUID entityGroupId;

    @Column(name = "entity_group_type")
    private String entityGroupType;

    @Column(name = "public_permission")
    private boolean publicPermission;

    @Column(name = "external_id")
    private UUID externalId;

    public GroupPermissionEntity() {
        super();
    }

    public GroupPermissionEntity(GroupPermission permission) {
        super(permission);
        this.tenantId = permission.getTenantId().getId();
        this.userGroupId = permission.getUserGroupId() != null ? permission.getUserGroupId().getId() : null;
        this.roleId = permission.getRoleId() != null ? permission.getRoleId().getId() : null;
        this.entityGroupId = permission.getEntityGroupId() != null ? permission.getEntityGroupId().getId() : null;
        this.entityGroupType = permission.getEntityGroupType() != null ? permission.getEntityGroupType().name() : null;
        this.publicPermission = permission.isPublicPermission();
        if (permission.getExternalId() != null) {
            this.externalId = permission.getExternalId().getId();
        }
    }

    @Override
    public GroupPermission toData() {
        GroupPermission permission = new GroupPermission(new GroupPermissionId(this.getUuid()));
        permission.setCreatedTime(createdTime);
        permission.setVersion(version);
        permission.setTenantId(TenantId.fromUUID(tenantId));
        if (userGroupId != null) {
            permission.setUserGroupId(new EntityGroupId(userGroupId));
        }
        if (roleId != null) {
            permission.setRoleId(new RoleId(roleId));
        }
        if (entityGroupId != null) {
            permission.setEntityGroupId(new EntityGroupId(entityGroupId));
        }
        if (entityGroupType != null) {
            permission.setEntityGroupType(EntityType.valueOf(entityGroupType));
        }
        permission.setPublicPermission(publicPermission);
        if (externalId != null) {
            permission.setExternalId(new GroupPermissionId(externalId));
        }
        return permission;
    }
}
