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
package org.thingsboard.server.dao.sql.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.model.sql.EntityGroupEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
public class JpaEntityGroupDao extends JpaAbstractDao<EntityGroupEntity, EntityGroup> implements EntityGroupDao {

    @Autowired
    private EntityGroupRepository entityGroupRepository;

    @Override
    protected Class<EntityGroupEntity> getEntityClass() {
        return EntityGroupEntity.class;
    }

    @Override
    protected JpaRepository<EntityGroupEntity, UUID> getRepository() {
        return entityGroupRepository;
    }

    @Override
    public EntityGroup save(TenantId tenantId, EntityGroup entityGroup) {
        return saveAndFlush(tenantId, entityGroup);
    }

    @Override
    public PageData<EntityGroup> findByOwnerAndType(UUID tenantId, UUID ownerId, String entityType, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupRepository.findByOwnerAndType(
                tenantId, ownerId, entityType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public EntityGroup findByOwnerAndTypeAndName(UUID tenantId, UUID ownerId, String entityType, String name) {
        return DaoUtil.getData(entityGroupRepository.findByTenantIdAndOwnerIdAndEntityTypeAndName(tenantId, ownerId, entityType, name));
    }

    @Override
    public EntityGroup findAllGroupByOwnerAndType(UUID ownerId, String entityType) {
        return DaoUtil.getData(entityGroupRepository.findAllGroupByOwnerAndType(ownerId, entityType));
    }

    @Override
    public List<EntityGroup> findGroupsByEntityId(UUID tenantId, UUID entityId, String entityType) {
        return DaoUtil.convertDataList(entityGroupRepository.findGroupsByEntityId(tenantId, entityId, entityType));
    }

    @Override
    public void addEntityToGroup(UUID groupId, UUID entityId, String entityType, long createdTime) {
        entityGroupRepository.addEntityToGroup(groupId, entityId, entityType, createdTime);
    }

    @Override
    public void removeEntityFromGroup(UUID groupId, UUID entityId) {
        entityGroupRepository.removeEntityFromGroup(groupId, entityId);
    }

    @Override
    public void removeEntityFromAllGroups(UUID tenantId, UUID entityId, String entityType) {
        entityGroupRepository.removeEntityFromAllGroups(tenantId, entityId, entityType);
    }

    @Override
    public boolean isEntityInGroup(UUID groupId, UUID entityId) {
        return entityGroupRepository.isEntityInGroup(groupId, entityId);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return 0L;
    }

    @Override
    public PageData<EntityGroup> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(entityGroupRepository.findByTenantId(
                tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }
}
