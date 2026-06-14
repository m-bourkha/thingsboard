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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.UUID;

@Component
@SqlDao
public class JpaGroupPermissionDao extends JpaAbstractDao<GroupPermissionEntity, GroupPermission> implements GroupPermissionDao {

    @Autowired
    private GroupPermissionRepository groupPermissionRepository;

    @Override
    protected Class<GroupPermissionEntity> getEntityClass() {
        return GroupPermissionEntity.class;
    }

    @Override
    protected JpaRepository<GroupPermissionEntity, UUID> getRepository() {
        return groupPermissionRepository;
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(groupPermissionRepository.findByTenantId(
                tenantId,
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(groupPermissionRepository.findByTenantIdAndUserGroupId(
                tenantId,
                userGroupId,
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(groupPermissionRepository.findByTenantIdAndEntityGroupId(
                tenantId,
                entityGroupId,
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(groupPermissionRepository.findByTenantIdAndRoleId(
                tenantId,
                roleId,
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleIdAndEntityGroupId(UUID tenantId, UUID userGroupId, UUID roleId, UUID entityGroupId) {
        GroupPermissionEntity entity;
        if (entityGroupId != null) {
            entity = groupPermissionRepository.findByTenantIdAndUserGroupIdAndRoleIdAndEntityGroupId(tenantId, userGroupId, roleId, entityGroupId);
        } else {
            entity = groupPermissionRepository.findByTenantIdAndUserGroupIdAndRoleIdAndEntityGroupIdIsNull(tenantId, userGroupId, roleId);
        }
        return Optional.ofNullable(DaoUtil.getData(entity));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return groupPermissionRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public PageData<GroupPermission> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

}
