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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class GroupPermissionDataValidator extends DataValidator<GroupPermission> {

    @Autowired
    private GroupPermissionDao groupPermissionDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, GroupPermission groupPermission) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.GROUP_PERMISSION);
        groupPermissionDao.findGroupPermissionByTenantIdAndUserGroupIdAndRoleIdAndEntityGroupId(
                tenantId.getId(),
                groupPermission.getUserGroupId().getId(),
                groupPermission.getRoleId().getId(),
                groupPermission.getEntityGroupId() != null ? groupPermission.getEntityGroupId().getId() : null
        ).ifPresent(p -> {
            throw new DataValidationException("Group permission already exists!");
        });
    }

    @Override
    protected GroupPermission validateUpdate(TenantId tenantId, GroupPermission groupPermission) {
        GroupPermission old = groupPermissionDao.findById(groupPermission.getTenantId(), groupPermission.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing group permission!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, GroupPermission groupPermission) {
        if (groupPermission.getTenantId() == null) {
            throw new DataValidationException("Group permission should be assigned to tenant!");
        } else if (!tenantService.tenantExists(groupPermission.getTenantId())) {
            throw new DataValidationException("Group permission is referencing to non-existent tenant!");
        }
        if (groupPermission.getUserGroupId() == null) {
            throw new DataValidationException("Group permission user group should be specified!");
        }
        EntityGroup userGroup = entityGroupDao.findById(tenantId, groupPermission.getUserGroupId().getId());
        if (userGroup == null) {
            throw new DataValidationException("Group permission is referencing to non-existent user group!");
        }
        if (!groupPermission.getTenantId().equals(userGroup.getTenantId())) {
            throw new DataValidationException("Group permission user group should belong to the same tenant!");
        }
        if (userGroup.getType() != EntityType.USER) {
            throw new DataValidationException("Group permission user group must be a USER entity group!");
        }
        if (groupPermission.getRoleId() == null) {
            throw new DataValidationException("Group permission role should be specified!");
        }
        Role role = roleDao.findById(tenantId, groupPermission.getRoleId().getId());
        if (role == null) {
            throw new DataValidationException("Group permission is referencing to non-existent role!");
        }
        if (!groupPermission.getTenantId().equals(role.getTenantId())) {
            throw new DataValidationException("Group permission role should belong to the same tenant!");
        }
        if (role.getType() == RoleType.GROUP && groupPermission.getEntityGroupId() == null) {
            throw new DataValidationException("Entity group should be specified for a group role permission!");
        }
        if (groupPermission.getEntityGroupId() != null) {
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, groupPermission.getEntityGroupId().getId());
            if (entityGroup == null) {
                throw new DataValidationException("Group permission is referencing to non-existent entity group!");
            }
            if (!groupPermission.getTenantId().equals(entityGroup.getTenantId())) {
                throw new DataValidationException("Group permission entity group should belong to the same tenant!");
            }
            if (groupPermission.getEntityGroupType() == null) {
                groupPermission.setEntityGroupType(entityGroup.getType());
            } else if (groupPermission.getEntityGroupType() != entityGroup.getType()) {
                throw new DataValidationException("Group permission entity group type does not match the referenced entity group!");
            }
        }
    }
}
