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
package org.thingsboard.server.dao.group;

import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface EntityGroupDao extends Dao<EntityGroup>, TenantEntityDao<EntityGroup> {

    EntityGroup save(TenantId tenantId, EntityGroup entityGroup);

    PageData<EntityGroup> findByOwnerAndType(UUID tenantId, UUID ownerId, String entityType, PageLink pageLink);

    EntityGroup findByOwnerAndTypeAndName(UUID tenantId, UUID ownerId, String entityType, String name);

    EntityGroup findAllGroupByOwnerAndType(UUID ownerId, String entityType);

    List<EntityGroup> findGroupsByEntityId(UUID tenantId, UUID entityId, String entityType);

    void addEntityToGroup(UUID groupId, UUID entityId, String entityType, long createdTime);

    void removeEntityFromGroup(UUID groupId, UUID entityId);

    void removeEntityFromAllGroups(UUID tenantId, UUID entityId, String entityType);

    boolean isEntityInGroup(UUID groupId, UUID entityId);

    PageData<EntityGroup> findByTenantId(UUID tenantId, PageLink pageLink);
}
