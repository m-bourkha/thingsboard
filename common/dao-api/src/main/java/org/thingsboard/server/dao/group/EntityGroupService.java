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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface EntityGroupService extends EntityDaoService {

    EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId);

    EntityGroup saveEntityGroup(EntityGroup entityGroup);

    void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<EntityGroup> findEntityGroupsByOwnerAndType(TenantId tenantId, EntityId ownerId, EntityType entityType, PageLink pageLink);

    List<EntityGroup> findEntityGroupsByEntityId(TenantId tenantId, EntityId entityId);

    EntityGroup findOrCreateAllGroup(TenantId tenantId, EntityId ownerId, EntityType entityType);

    void addEntityToGroup(EntityGroupId groupId, EntityId entityId);

    void removeEntityFromGroup(EntityGroupId groupId, EntityId entityId);

    void removeEntityFromAllGroups(TenantId tenantId, EntityId entityId);

    void deleteEntityGroupsByTenantId(TenantId tenantId);

    boolean isEntityInGroup(EntityGroupId groupId, EntityId entityId);

    EntityGroup makePublic(TenantId tenantId, EntityGroupId entityGroupId);

    EntityGroup makePrivate(TenantId tenantId, EntityGroupId entityGroupId);

    EntityGroup changeOwner(TenantId tenantId, EntityGroupId entityGroupId, EntityId newOwnerId);
}
